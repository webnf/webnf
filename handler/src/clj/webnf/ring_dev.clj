(ns webnf.ring-dev
  (:require [ring.util.response :refer [content-type response]]
            [clojure.pprint :refer [pprint *print-right-margin*]]
            [webnf.base :refer [pprint-str]]
            [clojure.tools.logging :as log]
            [clojure.core.rrb-vector :as fv]))

(defn escape-html [^String s]
  (.. s
      (replace "&" "&amp;")
      (replace "<" "&lt;")
      (replace ">" "&gt;")
      (replace "\"" "&quot;")))

(defn- when-wrap [val before after]
  (when val (concat [before] val [after])))

(defn pprint-html
  "Print data structure via pprint, escape and surround with <pre> tags.
   Optional chrome issues a full html page."
  ([data] (pprint-html data false))
  ([data {:as chrome :keys [title style script header body footer]}]
   (concat
    (when chrome ["<!DOCTYPE html>\n<html>"])
    (when (or title style script) ["<head>"])
    (when-wrap title "<title>" "<title>")
    (when-wrap style "<style>" "<style>")
    (when-wrap script "<style>" "<script>")
    (when (or title style script) ["</head>"])
    (when chrome ["<body>"])
    (when-wrap header "<h1>" "</h1>")
    (when-wrap body "<p>" "</p>")
    (list "<pre>" (escape-html (pprint-str data 120)) "</pre>")
    (when-wrap footer "<p>" "</p>")
    (when chrome ["</body></html>"]))))

(defn echo-handler
  "A simple echo handler for ring requests. PPrints the request to HTML."
  [req]
  (content-type
   (response
    (pprint-html (update req :body slurp)
                 {:header "Your request was:"}))
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
