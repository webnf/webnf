(ns webnf.ring-dev
  (:require [ring.util.response :refer [content-type response]]
            [clojure.pprint :refer [pprint *print-right-margin*]]
            [webnf.base :refer [pprint-str]]
            [clojure.tools.logging :as log]))

(defn echo-handler
  "A simple echo handler for ring requests. PPrints the request to HTML."
  [req]
  (content-type
   (response
    (str
     "<!DOCTYPE html><html><body><pre>\nYour request was:\n\n"
     (.. (binding [*print-right-margin* 120]
           (pprint-str (update-in req [:body] slurp)))
         (replace "&" "&amp;")
         (replace "<" "&lt;")
         (replace ">" "&gt;")
         (replace "\"" "&quot;"))
     "</pre></body></html>"))
   "text/html"))

(defn wrap-logging
  ([handler] (wrap-logging handler :trace))
  ([handler level]
     (fn [{:keys [request-method uri query-string] :as req}]
       (try
         (let [resp (handler req)]
           (log/log "http.request" level nil
                    (str (.toUpperCase (name request-method)) " "
                         uri " " query-string \newline
                         (pprint-str {:request req
                                      :response resp})))
           resp)
         (catch Exception e
           (log/error e (str (.toUpperCase (name request-method)) " " uri " " query-string 
                             " : Exception during handling of request\n" (pprint-str req)))
           (throw e))))))
