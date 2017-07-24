(ns webnf.js.xhr
  (:require [goog.Uri :as Uri]
            [goog.net.XhrIo :as XhrIo]
            [webnf.async.promise :refer [promise]]
            [webnf.async :refer [callback-read-port]]
            [webnf.base.logging :as log]
            [clojure.string :refer [split-lines]]
            [webnf.js :refer [to-js]]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]))

;; ### XHR

;; how is this not in gclosure?

(defn hmap
  "Parse the headers of an XmlHttpRequest"
  [hs]
  (persistent!
   (reduce (fn [res l]
             (if-let [[_ k v] (re-matches #"([^:]*):(.*)" l)]
               (assoc! res (str/lower-case (.trim k)) (.trim v))
               (do
                 (log/error "Not a header" l)
                 res)))
           (transient {}) (split-lines hs))))

(defn- as-str [o]
  (if (keyword? o)
    (name o)
    (str o)))

(s/def ::header-name string?) ;; TODO specify allowed characters

(s/def ::uri
  (s/conformer #(try (Uri/parse %)
                     (catch :default e ::s/invalid))
               #(.toString %)))

(s/def ::method (s/or :str (s/and string? #(re-matches #"[a-zA-Z]*" %))
                      :kw  (s/and keyword? #(re-matches #"[a-zA-Z]*" (name %)))))
(s/def ::params  (s/map-of string? string?))
(s/def ::headers (s/map-of ::header-name string?))
(s/def :webnf.xhr.request/body string?)
(s/def :webnf.xhr.options/xml boolean?)

(s/def ::options
  (s/keys
   :opt-un [::method ::params ::headers
            :webnf.xhr.request/body
            :webnf.xhr.options/xml]))

(s/def ::status (s/int-in 100 900))
(s/def ::xml-doc #(instance? js/XMLDocument %))

(s/def ::response-callback (s/fspec
                            :args (s/cat
                                   :status  ::http-status
                                   :headers ::headers
                                   :body    (s/or :text-body string? :xml-body ::xml-doc)
                                   :xhr     #(instance? js/XMLHttpRequest %))))

(s/fdef xhr
        :args (s/cat
               :request-uri ::uri
               :request-data (s/? ::options)
               :on-finish ::response-callback))

(defn xhr
  "Request an uri with clojure data, via XMLHttpRequest.

  Takes a callback, that will be called with [status headers body], returns a goog.net.XhrIo object.
  Takes headers and params as clojure data, passes headers as clojure data into callback."
  ([uri on-finish] (xhr uri nil on-finish))
  ([uri
    {:keys [method body headers params xml]}
    on-finish]
   (let [uri (Uri/parse uri)
         _ (reduce-kv (fn [_ param value]
                        (.setParameterValue uri param value))
                      nil params)
         headers (and headers (to-js headers))]
     (XhrIo/send uri #(let [t (.-target %)]
                        (on-finish (.getStatus t)
                                   (hmap (.getAllResponseHeaders t))
                                   (if xml
                                     (.getResponseXml t)
                                     (.getResponseText t))
                                   t))
                 (as-str method) body headers))))

(defn xhr-async
  "Request an uri with XmlHttpRequest, return channel that will
  receive response in ring format {:uri :status :headers :body}

  Request data can also be passed in ring format
  {:method :params :headers :body :always-refresh}
  :auto-refresh true triggers a request on every read
  :parse-response can be a custom response parser working directly on the XmlHttpRequest"
  ([uri] (xhr uri nil))
  ([uri {:keys [method body headers params auto-refresh parse-response xml] :as options}]
   (let [rp (callback-read-port (fn [result]
                                  (xhr uri options
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
