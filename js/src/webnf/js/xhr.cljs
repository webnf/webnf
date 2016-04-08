(ns webnf.js.xhr
  (:require [goog.Uri :as Uri]
            [goog.net.XhrIo :as XhrIo]
            [webnf.async.promise :refer [promise]]
            [webnf.async :refer [callback-read-port]]
            [webnf.base.logging :as log]
            [clojure.string :refer [split-lines]]
            [webnf.js :refer [to-js]]))

;; ### XHR

;; how is this not in gclosure?

(defn hmap
  "Parse the headers of an XmlHttpRequest"
  [hs]
  (persistent!
   (reduce (fn [res l]
             (if-let [[_ k v] (re-matches #"([^:]*):(.*)" l)]
               (assoc! res (.trim k) (.trim v))
               (do
                 (log/error "Not a header" l)
                 res)))
           (transient {}) (split-lines hs))))

(defn- parse-xhr-response [evt]
  (let [t (.-target evt)]
    {:uri (.getLastUri t)
     :status (.getStatus t)
     :headers (hmap (.getAllResponseHeaders t))
     :body (.getResponseText t)}))

(defn- parse-xhr-response-xml [evt]
  (let [t (.-target evt)]
    {:uri (.getLastUri t)
     :status (.getStatus t)
     :headers (hmap (.getAllResponseHeaders t))
     :body (.getResponseXml t)}))

(defn- as-str [o]
  (if (keyword? o)
    (name o)
    (str o)))

(defn xhr
  "Request an uri with XmlHttpRequest, return channel that will
  receive response in ring format {:uri :status :headers :body}

  Request data can also be passed in ring format
  {:method :params :headers :body :always-refresh}
  :auto-refresh true triggers a request on every read
  :parse-response can be a custom response parser working directly on the XmlHttpRequest"
  ([uri] (xhr uri nil))
  ([uri {:keys [method body headers params auto-refresh parse-response xml]}]
   (let [uri (Uri/parse uri)
         parser (if parse-response
                  (comp parse-response #(.-target %))
                  (if xml parse-xhr-response-xml parse-xhr-response))
         _ (reduce-kv (fn [_ param value]
                        (.setParameterValue uri param value))
                      nil params)
         headers (and headers (to-js headers))
         rp (callback-read-port (fn [result]
                                  (XhrIo/send uri (comp result parser)
                                              (as-str method) body headers)))]
     (if auto-refresh
       rp (promise rp)))))
