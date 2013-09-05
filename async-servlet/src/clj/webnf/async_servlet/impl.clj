(ns webnf.async-servlet.impl
  (:import
   (javax.servlet AsyncListener AsyncContext AsyncEvent ServletConfig)
   (javax.servlet.http HttpServlet HttpServletRequest HttpServletResponse))
  (:require [ring.util.servlet :as servlet]
            [clojure.tools.logging :as log]))

(set! *warn-on-reflection* true)

(defn resolve-config-var [^ServletConfig config ^String name]
  (when-let [vname (.getInitParameter config name)]
    (let [vsym (symbol vname)
          ns (symbol (namespace vsym))]
      (require ns)
      (resolve vsym))))

(def ^:private write-chunk @#'servlet/set-body)

(defn handle-servlet-request [handler ^HttpServletRequest request response]
  (let [request-map (assoc (servlet/build-request-map request)
                      :path-info (.getPathInfo request))
        response-map (handler request-map)]
    (if-let [make-net-listener (:async response-map)]
      (let [ac (.startAsync request)
            r  (.getResponse ac)
            user-listener (fn [& {:keys [status headers chunk body] :as opts}]
                            {:pre [(not (and chunk body))]}
                            (when status
                              (servlet/set-status r status))
                            (when headers
                              (servlet/set-headers r headers))
                            (when chunk
                              (write-chunk r chunk))
                            (when body
                              (write-chunk r body))
                            (when (contains? opts :body)
                              (.complete ac)))
            {:keys [init start-async error timeout complete] :as net-listener}
            (make-net-listener user-listener)]
        (when-not (every? fn? [init start-async error timeout complete])
          (log/debug "Not all servlet event handlers are set:"
                     :init init :start-async start-async
                     :error error :timeout timeout :complete complete))
        (if-let [to (:timeout response-map)]
          (.setTimeout ac to)
          (log/debug "No timeout set on async response"))
        (log/tracef "Started async response with timeout: %.1fs"
                    (double (/ (.getTimeout ac) 1000)))
        (.addListener ac (reify AsyncListener
                           (onStartAsync [this event]
                             (when start-async
                               (start-async event user-listener)))
                           (onError [this event]
                             (when error
                               (error event user-listener)))
                           (onTimeout [this event]
                             (when timeout
                               (timeout event user-listener)))
                           (onComplete [this event]
                             (when complete
                               (complete event user-listener)))))
        (when init (init (AsyncEvent. ac) user-listener)))
      (servlet/update-servlet-response response response-map))))



