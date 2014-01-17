(ns webnf.server
  (:require 
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [ring.util.servlet :as servlet]
;   [webnf.async-servlet.impl :as asi]
   )
  (:import
   org.jfastcgi.servlet.FastCGIServlet
   (org.eclipse.jetty.server Server Request)
   (org.eclipse.jetty.servlet ServletContextHandler ServletHolder DefaultServlet)
   (org.eclipse.jetty.server HttpConfiguration ServerConnector HttpConnectionFactory
                             ConnectionFactory)
;   (org.eclipse.jetty.spdy.server.http HTTPSPDYServerConnector)
   (org.eclipse.jetty.server.handler HandlerCollection HandlerList)
   (org.eclipse.jetty.util.thread ExecutorThreadPool)
   (org.eclipse.jetty.server.handler AbstractHandler)
   (org.eclipse.jetty.webapp WebAppContext)
   (org.eclipse.jetty.util.thread QueuedThreadPool)
   (org.eclipse.jetty.util.ssl SslContextFactory)))

(set! *warn-on-reflection* true)

(comment
  (defn- proxy-handler
    "Returns an Jetty Handler implementation for the given Ring handler."
    [handler]
    (proxy [AbstractHandler] []
      (handle [_ ^Request base-request request response]
        (asi/handle-servlet-request handler request response)
        (.setHandled base-request true))))
  (defn make-ring-handler [handler mapping]
    (doto (ServletContextHandler.)
      (.addServlet (doto (ServletHolder.)
                     (.setServletHandler (proxy-handler handler))
                     (.setAsyncSupported true))
                   mapping))))

(defn make-fcgi-handler [address fcgi-mapping cwd]
  (doto (ServletContextHandler.)
    (.setResourceBase cwd)
    (.setWelcomeFiles (into-array String ["index.php" "index.html"]))
    (.addServlet (doto (ServletHolder. FastCGIServlet)
                   (.setInitParameters {"server-address" address}))
                 fcgi-mapping)
    (.addServlet (doto (ServletHolder. DefaultServlet)
                   (.setInitParameters {"dirAllowed" "true"}))
                 "/")))

(defn make-servlet-handler [servlet-class params mapping]
  (doto (ServletContextHandler.)
    (.addServlet (doto (ServletHolder. servlet-class)
                   (.setInitParameters params)
                   (.setAsyncSupported true))
                 mapping)
    (.setClassLoader (.getClassLoader servlet-class))))

(defn make-war-handler [app-path war-path mapping class-loader]
  (doto (WebAppContext. (.getCanonicalPath (io/file app-path)) mapping)
    (.setWar war-path)))

(defn add-vhost! 
  ([{:keys [jetty container handlers vhosts] :as ctx} 
    id add-vhosts servlet-context-handler]
     (when (get handlers id)
       (throw (ex-info (str "Handler " id " is already running" {:id id}))))
     (when-let [have (seq (set/intersection vhosts (set add-vhosts)))]
       (throw (ex-info (str "Vhosts " (str/join ", " have) " are already mapped")
                       {:id id :add-vhosts add-vhosts :vhosts vhosts})))
     (.addHandler container (doto servlet-context-handler
                              (.setVirtualHosts (into-array String add-vhosts))))
     (-> ctx
         (update-in [:vhosts] into add-vhosts)
         (assoc-in  [:handlers id] servlet-context-handler))))

(defn remove-vhost! [{:keys [jetty container handlers vhosts] :as ctx}
                     id]
  (if-let [handler ^ServletContextHandler (handlers id)]
    (let  [remove-vhosts (set (.getVirtualHosts handler))]
      (.removeHandler container handler)
      (-> ctx
          (update-in [:vhosts] set/difference remove-vhosts)
          (update-in [:handlers] dissoc id)))
    (throw (ex-info (str "No handler with " id) {:id id}))))

(defn server [{:keys [host port idle-timeout header-size
                      default-handler min-threads max-threads
                      ssl? ssl-port keystore key-password
                      truststore trust-password client-auth]
               :or {port 80 idle-timeout 200000 header-size 16384
                    min-threads 4 max-threads 42}}]
  (let [container (HandlerCollection. true)
        conn-facts (into-array ConnectionFactory
                               [(HttpConnectionFactory. 
                                 (doto (HttpConfiguration.)
                                   (.setSendDateHeader true)
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
    {:jetty (doto server
              (.setHandler (if default-handler
                             (doto (HandlerList.)
                               (.addHandler container)
                               (.addHandler default-handler))
                             container))
              (.setStopTimeout 1000)
              (.setStopAtShutdown true))
     :container container
     :vhosts #{}
     :handlers {}}))

(defn start! [{:keys [^Server jetty] :as ctx}]
  (.start jetty)
  ctx)

(defn stop! [{:keys [^Server jetty] :as ctx}]
  (.stop jetty)
  (.join jetty)
  ctx)
