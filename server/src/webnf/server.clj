(ns webnf.server
  (:require 
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [ring.util.servlet :as servlet]
   [clojure.tools.logging :as log]
;   [webnf.async-servlet.impl :as asi]
   )
  (:import
   (java.util Date Collection UUID EnumSet)
   (javax.servlet Filter DispatcherType)
   (javax.servlet.http Cookie HttpServletRequest HttpServletResponse)
   org.jfastcgi.servlet.FastCGIServlet
   (org.eclipse.jetty.server Server Request)
   (org.eclipse.jetty.servlet ServletContextHandler ServletHolder DefaultServlet FilterHolder)
   (org.eclipse.jetty.server HttpConfiguration ServerConnector HttpConnectionFactory
                             ConnectionFactory RequestLog)
;   (org.eclipse.jetty.spdy.server.http HTTPSPDYServerConnector)
   (org.eclipse.jetty.server.handler HandlerCollection HandlerList RequestLogHandler)
   (org.eclipse.jetty.util.thread ExecutorThreadPool)
   (org.eclipse.jetty.util.component LifeCycle LifeCycle$Listener)
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

(defn make-servlet-handler [servlet-class params mapping
                            & {:keys [lifecycle-listener]}]
  (cond-> (doto (ServletContextHandler.)
            (.addServlet (doto (ServletHolder. servlet-class)
                           (.setInitParameters params)
                           (.setAsyncSupported true))
                         mapping)
            (.setClassLoader (.getClassLoader servlet-class)))
          lifecycle-listener (doto (.addLifeCycleListener lifecycle-listener))))

(defn make-lifecycle-listener [& {:keys [^Runnable starting
                                         ^Runnable started
                                         ^Runnable stopping
                                         ^Runnable stopped
                                         ^Runnable failure]}]
  (reify LifeCycle$Listener
    (lifeCycleStarting [_ event]
      (log/trace "LifeCycle Starting" event)
      (when starting (.run starting)))
    (lifeCycleStarted [_ event]
      (log/trace "LifeCycle Started" event)
      (when started (.run started)))
    (lifeCycleStopping [_ event]
      (log/trace "LifeCycle Stopping" event)
      (when stopping (.run stopping)))
    (lifeCycleStopped [_ event]
      (log/trace "LifeCycle Stopped" event)
      (when stopped (.run stopped)))
    (lifeCycleFailure [_ event cause]
      (log/debug cause "LifeCycle Failure" event)
      (when failure (.run failure)))))

(defn make-war-handler [app-path war-path mapping class-loader]
  (doto (WebAppContext. (.getCanonicalPath (io/file app-path)) mapping)
    (.setWar war-path)))

(defmacro assoc* [m & {:as kvs}]
  (let [syms (take (count kvs) (repeatedly gensym))]
    `(let ~(vec (interleave syms
                            (vals kvs))) 
       (cond-> ~m ~@(mapcat (fn [sym key]
                             [`(not (nil? ~sym))
                              `(assoc ~key ~sym)])
                           syms (keys kvs))))))

(defmacro headers [to-seq re exclude?]
  `(let [re# ~re
         ex?# ~exclude?]
     (into {} (for [H# (~to-seq (.getHeaderNames re#))
                    :let [h# (.toLowerCase ^String H#)]
                    :when (not (ex?# h#))]
                [h# (.getHeader re# h#)]))))

(defmacro when-size [v]
  `(let [v# ~v]
     (when (>= v# 0) v#)))

(defn from-cookie [^Cookie c]
  (assoc* {:name (.getName c)
           :value (.getValue c)
           :http-only (.isHttpOnly c)}
          :path (.getPath c)
          :secure (.getSecure c)
          :comment (.getComment c)
          :domain (.getDomain c)
          :max-age (when-size (.getMaxAge c))
          :version (when-size (.getVersion c))))

(defn collection-seq [^Collection c]
  (iterator-seq (.iterator c)))

(defn make-request-logger [logger-fn ^LifeCycle handler]
  (doto (RequestLogHandler.)
    (.setHandler handler)
    (.setRequestLog (reify RequestLog
                      (log [_ request response]
                        (logger-fn
                         (-> {:request/protocol (.getProtocol request)
                              :request/scheme (.getScheme request)
                              :request/time (Date. (.getTimeStamp request))
                              :request/duration (- (System/currentTimeMillis)
                                                   (.getTimeStamp request))
                              :request/host (.getServerName request)
                              :request/port (.getServerPort request)
                              :remote/ip (.getRemoteAddr request)
                              :remote/host (.getRemoteHost request)
                              :remote/port (.getRemotePort request)

                              :request.http/method (.toUpperCase (.getMethod request))
                              :request.http/path (.getPath (.getUri request))
                              :request.http/headers (headers enumeration-seq request #{"authorization" ;"cookie" "host"
                                                                                       })

                              :response.http/status (.getStatus response)
                              :response.http/headers (headers collection-seq response #{"x-request-id"})

                              :request.servlet/content-read (.getContentRead request)
                              :response.servlet/content-written (.getContentCount response)}
                             (assoc*
                              :remote/user (.getRemoteUser request)
                              :request/client-id (.getAttribute request "webnf.client.id")
                              :request/session-id (.getAttribute request "webnf.session.id")
                              :request/id (.getAttribute request "webnf.request.id")
                              :request.http/auth-type (.getAuthType request)
                              :request.http/cookies (seq (map from-cookie (.getCookies request)))
                              :request.http/query-params (.getQueryString request)))))
                      (addLifeCycleListener [_ listener] (.addLifeCycleListener handler listener))
                      (removeLifeCycleListener [_ listener] (.removeLifeCycleListener handler listener))
                      (isFailed [_] (.isFailed handler))
                      (isRunning [_] (.isRunning handler))
                      (isStarting [_] (.isStarting handler))
                      (isStarted [_] (.isStarted handler))
                      (isStopping [_] (.isStopping handler))
                      (isStopped [_] (.isStopped handler))
                      (start [_] (.start handler))
                      (stop [_] (.stop handler))))))

(defn uuid-cookie [^HttpServletRequest req name]
  (try (when-let [sid (some #(and (= name (.getName ^Cookie %))
                                  (.getValue %))
                            (.getCookies req))]
         (def last-cookies (.getCookies req))
         (UUID/fromString sid))
       (catch Exception e (log/trace e "Parse UUID"))))

(defn ensure-uuid-cookie! [^HttpServletRequest req ^HttpServletResponse resp cookie-name attr-name max-age]
  (let [id (or (uuid-cookie req cookie-name)
               (UUID/randomUUID))
        cookie (Cookie. cookie-name (str id))]
    (log/trace "Ensuring client cookie" cookie-name id)
    (.setAttribute req attr-name id)
    (.setMaxAge cookie max-age)
    (.setPath cookie "/")
    (.addCookie resp cookie)))

(defn make-id-filter [conf]
  (let [{:strs [client-cookie session-cookie request-id-header]} conf]
    (reify Filter
      (init [_ config])
      (doFilter [_ req resp chain]
        (try
          (log/trace "Filtering!!" conf
                     (.getAttribute req "webnf.client.id")
                     (.getAttribute req "webnf.session.id")
                     (.getAttribute req "webnf.request.id"))
          (when (and client-cookie (nil? (.getAttribute req "webnf.client.id")))
            (ensure-uuid-cookie! req resp client-cookie "webnf.client.id" (* 60 60 24 356)))
          (when (and session-cookie (nil? (.getAttribute req "webnf.session.id")))
            (ensure-uuid-cookie! req resp session-cookie "webnf.session.id" -1))
          (when (and request-id-header (nil? (.getAttribute req "webnf.request.id")))
            (let [id (UUID/randomUUID)]
              (.setHeader resp request-id-header (str id))
              (.setAttribute req "webnf.request.id" id)))
          (.doFilter chain req resp)
          (catch Exception e
            (log/error e "In id-filter")
            (throw e))))
      (destroy [_]))))

(defn add-vhost! 
  ([{:keys [jetty container handlers vhosts identify] :as ctx} 
    id add-vhosts servlet-context-handler]
     (when (get handlers id)
       (throw (ex-info (str "Handler " id " is already running" {:id id}))))
     (when-let [have (seq (set/intersection vhosts (set add-vhosts)))]
       (throw (ex-info (str "Vhosts " (str/join ", " have) " are already mapped")
                       {:id id :add-vhosts add-vhosts :vhosts vhosts})))
     (.addHandler container
                  (cond-> servlet-context-handler
                          identify (doto (.addFilter (FilterHolder. (make-id-filter identify)) "*"
                                                     (EnumSet/of DispatcherType/REQUEST DispatcherType/INCLUDE
                                                                 DispatcherType/FORWARD DispatcherType/ERROR
                                                                 DispatcherType/ASYNC)))
                          true (doto (.setVirtualHosts (into-array String add-vhosts)))))
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
                      truststore trust-password client-auth
                      request-logger identify]
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
              (.setHandler (let [handler (if default-handler
                                           (doto (HandlerList.)
                                             (.addHandler container)
                                             (.addHandler default-handler))
                                           container)]
                             (if request-logger
                               (make-request-logger request-logger handler)
                               handler)))
              (.setStopTimeout 1000)
              (.setStopAtShutdown true))
     :identify identify
     :container container
     :vhosts #{}
     :handlers {}}))

(defn start! [{:keys [^Server jetty] :as ctx}]
  (.start jetty)
  ctx)

(defn stop! [{:keys [^Server jetty] :as ctx}]
  (println ctx)
  (.stop jetty)
  (.join jetty)
  ctx)
