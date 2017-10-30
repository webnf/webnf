(ns webnf.js.xhr
  (:import goog.net.XhrIo)
  (:require [goog.Uri :as Uri]
            [goog.net.EventType :as NET]
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

(s/fdef request
        :args (s/cat
               :request-uri ::uri
               :request-data (s/? ::options)
               :on-finish ::response-callback))

;; Feature detect XMLHttpRequest.responseType

(defn request
  "Request an uri with clojure data, via XMLHttpRequest.

  Takes a callback, that will be called with [status headers body], returns a goog.net.XhrIo object.
  Takes headers and params as clojure data, passes headers as clojure data into callback."
  ([uri on-finish] (request uri nil on-finish))
  ([uri
    {:keys [method body headers params response-type on-progress timeout with-credentials]
     :or {method "GET"}}
    on-finish]
   (letfn [(on-complete [event]
             (let [request (.-target event)]
               (on-finish (.getStatus request)
                          (hmap (.getAllResponseHeaders request))
                          (.getResponse request)
                          request)))
           (cleanup     [event]
             (.dispose (.-target event)))]
     (doto (XhrIo.)
       (.listenOnce NET/COMPLETE on-complete)
       (.listenOnce NET/READY cleanup)
       (.setProgressEventsEnabled (boolean on-progress))
       (cond-> timeout (.setTimeoutInterval timeout))
       (cond-> response-type (.setResponseType response-type))
       (cond-> with-credentials (.setWithCredentials true))
       (cond-> on-progress (.listen NET/PROGRESS on-progress))
       (.send (if (empty? params)
                uri
                (reduce-kv #(doto %1 (.setParameterValue %2 %3))
                           (Uri/parse uri) params))
              (as-str method)
              body
              (and headers (to-js headers)))))))
