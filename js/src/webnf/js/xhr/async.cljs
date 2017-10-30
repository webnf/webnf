(ns webnf.js.xhr.async
  (:require [webnf.js.xhr :as xhr]
            [webnf.async.promise :refer [promise]]
            [webnf.async :refer [callback-read-port]]))

(defn request
  "Request an uri with XmlHttpRequest, return channel that will
  receive response in ring format {:uri :status :headers :body}

  Request data can also be passed in ring format
  {:method :params :headers :body :always-refresh}
  :auto-refresh true triggers a request on every read
  :parse-response can be a custom response parser working directly on the XmlHttpRequest

  see webnf.js.xhr/request for more options"
  ([uri] (request uri nil))
  ([uri {:keys [auto-refresh parse-response] :as options}]
   (let [rp (callback-read-port (fn [result]
                                  (xhr/request uri options
                                               (fn [status headers body target]
                                                 (result
                                                  (if parse-response
                                                    (parse-response target)
                                                    {:uri (.getLastUri target)
                                                     :status status
                                                     :headers headers
                                                     :body body}))))))]
     (if auto-refresh
       rp (promise rp)))))
