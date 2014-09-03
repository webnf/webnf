(ns webnf.server
  (:require 
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [ring.util.servlet :as servlet]
   [clojure.tools.logging :as log]
   [webnf.server.util :as util]
   [webnf.server.component :as scmp]
   [com.stuartsierra.component :as cmp])
  (:import
   (java.util Date Collection UUID EnumSet)
   (javax.servlet Filter DispatcherType)
   (javax.servlet.http Cookie HttpServletRequest HttpServletResponse)
   (org.eclipse.jetty.fcgi.server.proxy FastCGIProxyServlet)
   (org.eclipse.jetty.server Server Request)
   (org.eclipse.jetty.servlet ServletContextHandler ServletHolder DefaultServlet FilterHolder)
   (org.eclipse.jetty.server HttpConfiguration ServerConnector HttpConnectionFactory
                             ConnectionFactory RequestLog)
                                        ;   (org.eclipse.jetty.spdy.server.http HTTPSPDYServerConnector)
   (org.eclipse.jetty.server.handler HandlerCollection HandlerList RequestLogHandler)
   (org.eclipse.jetty.util.thread ExecutorThreadPool)
   (org.eclipse.jetty.util.component LifeCycle LifeCycle$Listener)
   (org.eclipse.jetty.server.handler AbstractHandler)
   (org.eclipse.jetty.webapp WebAppContext WebAppClassLoader)
   (org.eclipse.jetty.util.thread QueuedThreadPool)
   (org.eclipse.jetty.util.ssl SslContextFactory)))

(set! *warn-on-reflection* false)

(defn fcgi-handler [address fcgi-mapping cwd]
  (doto (ServletContextHandler.)
    (.setResourceBase cwd)
    (.setWelcomeFiles (into-array String ["index.php" "index.html"]))
    (.addServlet (doto (ServletHolder. FastCGIProxyServlet)
                   (.setInitParameters {"proxyTo" address
                                        "scriptRoot" cwd}))
                 fcgi-mapping)
    (.addServlet (doto (ServletHolder. DefaultServlet)
                   (.setInitParameters {"dirAllowed" "true"}))
                 "/")))

(defn servlet-handler [servlet-class params mapping
                       & {:keys [lifecycle-listener]}]
  (cond-> (doto (ServletContextHandler.)
            (.addServlet (doto (ServletHolder. servlet-class)
                           (.setInitParameters params)
                           (.setAsyncSupported true))
                         mapping)
            (.setClassLoader (.getClassLoader servlet-class)))
          lifecycle-listener (doto (.addLifeCycleListener lifecycle-listener))))

(defn- map-vars [syms cbs]
  (let [ns (create-ns 'webnf.server.auto)]
    (doall (map #(intern ns %1 %2)
                syms cbs))))

(defn- unmap-vars [syms]
  (let [ns (create-ns 'webnf.server.auto)]
    (dorun (map #(ns-unmap ns %) syms))))

(defn ring-handler [service & {:keys [init destroy]}]
  (let [cbs [init destroy service]
        [init-s destroy-s service-s :as syms] (repeatedly (count cbs) #(gensym "callback-"))]
    (servlet-handler webnf.AsyncServlet
                     {"webnf.handler.init" (when init (str "webnf.server.auto/" init-s))
                      "webnf.handler.service" (when service (str "webnf.server.auto/" service-s))
                      "webnf.handler.destroy" (when destroy (str "webnf.server.auto/" destroy-s))}
                     "/" :lifecycle-listener (util/make-lifecycle-listener
                                              :starting #(map-vars syms cbs)
                                              :stopped #(unmap-vars syms)))))

(defn war-handler [app-path war-path mapping #_class-loader
                   & {:keys [add-system-classes]}]
  (let [wac (WebAppContext. (.getCanonicalPath (io/file app-path)) mapping)
        ;; wcl (WebAppClassLoader. class-loader wac)
        ]
    (doseq [sc add-system-classes]
      (.addSystemClass wac sc))
    (doto wac
      (.setWar war-path)
      ;; (.setClassLoader wcl)
      ;; (.setParentLoaderPriority true)
      )))


(defn host [& {:keys [id handler vhosts]}]
  (cmp/using
   (scmp/map->HostComponent
    {:id id
     :handler (doto handler
                (.setVirtualHosts (into-array String vhosts)))})
   {:container ::container}))

(defn server [& {:keys [host port idle-timeout header-size
                        default-handler min-threads max-threads
                        ssl? ssl-port keystore key-password
                        truststore trust-password client-auth
                        identify logging-queue]
                 :or {port 80 idle-timeout 200000 header-size 16384
                      min-threads 4 max-threads 42}}]
  (let [container (HandlerCollection. true)
        conn-facts (into-array ConnectionFactory
                               [(HttpConnectionFactory. 
                                 (doto (HttpConfiguration.)
                                   (.setSendDateHeader false)
                                   (.setSendServerVersion false)
                                   (.setRequestHeaderSize header-size)
                                   (.setResponseHeaderSize header-size)))])
        server (Server. (QueuedThreadPool. max-threads min-threads))]
    (.addConnector server (doto (ServerConnector. server conn-facts)
                            (.setPort port)
                            (.setHost host)
                            (.setIdleTimeout idle-timeout)))
    (when (or ssl? ssl-port)
      (let [ssl-context-fact (SslContextFactory.)]
        (when keystore
          (.setKeyStorePath ssl-context-fact keystore))
        (when key-password
          (.setKeyStorePassword ssl-context-fact key-password))
        (when truststore
          (.setTrustStorePath ssl-context-fact truststore))
        (when trust-password
          (.setTrustStorePassword ssl-context-fact trust-password))
        (case client-auth
          :need (.setNeedClientAuth ssl-context-fact true)
          :want (.setWantClientAuth ssl-context-fact true)
          nil)
        (.addConnector server (doto (ServerConnector. server ssl-context-fact conn-facts)
                                (.setPort (or ssl-port 443))
                                (.setHost host)
                                (.setIdleTimeout idle-timeout)))))
    (scmp/map->ServerComponent
     {:jetty (doto server
               (.setStopTimeout 1000)
               (.setStopAtShutdown true))
      :container container
      :default-handler default-handler
      :identify identify
      :logging-queue logging-queue
      :hosts (cmp/using (cmp/system-map ::container container)
                        {::container :container})
      :vhosts #{}})))


(defn add-host [{:as server :keys [hosts vhosts jetty]}
                {:as host-cmp :keys [id handler]}]
  (let [cmp-vhosts (set (.getVirtualHosts handler))]
    (when (get hosts id)
      (throw (ex-info (str "Host " id " is already running" {:id id}))))
    (when-let [have (seq (set/intersection vhosts cmp-vhosts))]
      (throw (ex-info (str "Vhosts " (str/join ", " have) " are already mapped")
                      {:id id :cmp-vhosts cmp-vhosts :vhosts vhosts})))
    (-> server
        (assoc :hosts  (assoc hosts id host-cmp)
               :vhosts (into vhosts cmp-vhosts))
        (cond-> (.isRunning jetty)
                (update-in [:hosts] cmp/update-system [id] #'cmp/start)))))

(defn remove-host [{:as server :keys [hosts vhosts jetty]}
                   id]
  (let [host (or (get hosts id)
                 (throw (ex-info (str "No handler with " id) {:id id})))
        cmp-vhosts (set (.getVirtualHosts (:handler host)))]
    (-> server
        (cond-> (.isRunning jetty)
                (update-in [:hosts] cmp/update-system [id] #'cmp/stop))
        (assoc :hosts (dissoc hosts id)
               :vhosts (set/difference vhosts cmp-vhosts)))))
