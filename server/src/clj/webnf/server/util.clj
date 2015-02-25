(ns webnf.server.util
  (:require [webnf.kv :refer [assoc-when*]]
            [clojure.tools.logging :as log])
  (:import
   java.util.Queue
   webnf.server.JettyInterceptor
   (java.util Date Collection UUID EnumSet)
   (javax.servlet Filter DispatcherType)
   (javax.servlet.http Cookie HttpServletRequest HttpServletResponse)
   (org.eclipse.jetty.server Server Request Response)
   (org.eclipse.jetty.servlet ServletContextHandler ServletHolder DefaultServlet FilterHolder)
   (org.eclipse.jetty.server HttpConfiguration ServerConnector HttpConnectionFactory
                             ConnectionFactory RequestLog)
   (org.eclipse.jetty.server.handler HandlerCollection HandlerList RequestLogHandler)
   (org.eclipse.jetty.util.thread ExecutorThreadPool)
   (org.eclipse.jetty.util.component LifeCycle LifeCycle$Listener)
   (org.eclipse.jetty.server.handler AbstractHandler)
   (org.eclipse.jetty.webapp WebAppContext)
   (org.eclipse.jetty.util.thread QueuedThreadPool)
   (org.eclipse.jetty.util.ssl SslContextFactory)))

(set! *warn-on-reflection* true)

(defmacro when-size [v]
  `(let [v# ~v]
     (when (>= v# 0) v#)))

(defn from-cookie [^Cookie c]
  (assoc-when* {:name (.getName c)
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

(defn uuid-cookie [^HttpServletRequest req name]
  (try (when-let [sid (some #(and (= name (.getName ^Cookie %))
                                  (.getValue ^Cookie %))
                            (.getCookies req))]
         (UUID/fromString sid))
       (catch Exception e (log/trace e "Parse UUID"))))

(defn ensure-uuid-cookie! [^HttpServletRequest req ^HttpServletResponse resp cookie-name attr-name max-age uuid-provider]
  (let [id (or (uuid-cookie req cookie-name)
               (let [id (uuid-provider)]
                 (log/trace "Ensuring client cookie" cookie-name id)
                 id))
        cookie (Cookie. cookie-name (str id))]
    (.setAttribute req attr-name id)
    (.setMaxAge cookie max-age)
    (.setPath cookie "/")
    (.addCookie resp cookie)))


(defmacro headers [to-seq re exclude?]
  `(let [re# ~re
         to-seq# ~to-seq
         ex?# ~exclude?]
     (into {} (for [H# (to-seq# (.getHeaderNames re#))
                    :let [h# (.toLowerCase ^String H#)]
                    :when (not (ex?# h#))]
                [h# (doall (to-seq# (.getHeaders re# h#)))]))))

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

(defn ensure-uuids [^HttpServletRequest req ^HttpServletResponse resp
                    client-cookie session-cookie request-id-header uuid-provider]
  (try
    (when (and client-cookie (nil? (.getAttribute req "webnf.client.id")))
      (ensure-uuid-cookie! req resp client-cookie "webnf.client.id" (* 60 60 24 356) uuid-provider))
    (when (and session-cookie (nil? (.getAttribute req "webnf.session.id")))
      (ensure-uuid-cookie! req resp session-cookie "webnf.session.id" -1 uuid-provider))
    (when (and request-id-header (nil? (.getAttribute req "webnf.request.id")))
      (let [id (uuid-provider)]
        (.setHeader resp ^String request-id-header (str id))
        (.setAttribute req "webnf.request.id" id)))
    (catch Exception e
      (log/error e "In id-ceptor")
      (throw e))))

(defn request-id-ceptor
  ([{:keys [client-cookie session-cookie request-id-header uuid-provider] :as conf
     :or {uuid-provider #(UUID/randomUUID)}}]
     (reify JettyInterceptor
       (onEvent [_ req resp]
         (ensure-uuids req resp client-cookie session-cookie request-id-header uuid-provider)))))

(defn reify-request [^Request request ^Response response]
  (-> {:request/protocol (.getProtocol request)
       :request/scheme (.getScheme request)
       :request/time (Date. (.getTimeStamp request))
       :request/duration (- (System/currentTimeMillis)
                            (.getTimeStamp request))
       :request/host (.getServerName request)
       :request/port (.getServerPort request)
       :remote/ip (.getRemoteAddr request)
       :remote/port (.getRemotePort request)

       :request.http/method (.toUpperCase (.getMethod request))
       :request.http/path (.getRequestURI request)
       :request.http/headers (headers enumeration-seq request #{"authorization" ;"cookie" "host"
                                                                })

       :response.http/status (.getStatus response)
       :response.http/headers (headers collection-seq response #{ ;"x-request-id"
                                                                 })

       :request/content-read (.getContentRead request)
       :response/content-written (.getContentCount response)}
      (assoc-when*
       :remote/user (.getRemoteUser request)
       :client.webnf/id (.getAttribute request "webnf.client.id")
       :session.webnf/id (.getAttribute request "webnf.session.id")
       :request.webnf/id (.getAttribute request "webnf.request.id")
       :request.http/auth-type (.getAuthType request)
       :request.http/cookies (seq (map from-cookie (.getCookies request)))
       :request.http/query-string (.getQueryString request))))

(defn request-log-ceptor [^Queue queue]
  (reify JettyInterceptor
    (onEvent [_ req resp]
      (let [entry (reify-request req resp)]
        (when-not (.offer queue entry)
          (log/error "Request log queue is full; discarding" (pr-str entry)))))))
