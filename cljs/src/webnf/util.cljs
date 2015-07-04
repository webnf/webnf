(ns webnf.util
  (:require
   [clojure.string :refer [split-lines split join]]
   [goog.dom :as dom]
   [goog.events :as evt]
   [goog.string :as gstr]
   [goog.events.EventType :as ET]
   [goog.net.EventType :as NET]
   [goog.net.XhrIo :as XhrIo]
   [goog.Uri :as Uri]
   [cljs.core.async :refer [chan >! close!]]
   [cljs.core.async.impl.protocols :as asyncp :refer [ReadPort WritePort]]
   [webnf.channel :refer [callback-read-port]]
   [webnf.promise :refer [promise]]
   [webnf.impl :as impl]
   webnfjs.jszip webnfjs.file-saver)
  (:require-macros 
   [cljs.core.async.macros :refer [go]]))

(def log impl/log)
(def log-pr impl/log-pr)

(defn scat
  "Returns a function taking a seq on which f is applied.
   To [scat]ter is an antonym of to [juxt]apose."
  [f]
  #(apply f %1))

(def to-js
  "Makes a js object from a map"
  (comp (scat js-obj) (scat concat)))

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
                 (log "ERROR Not a header" l)
                 res)))
           (transient {}) (split-lines hs))))

(defn- parse-xhr-response [evt]
  (let [t (.-target evt)]
    {:uri (.getLastUri t)
     :status (.getStatus t)
     :headers (hmap (.getAllResponseHeaders t))
     :body (.getResponseText t)}))

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
  ([uri {:keys [method body headers params auto-refresh parse-response]}]
   (let [uri (Uri/parse uri)
         parser (if parse-response
                  (comp parse-response #(.-target %))
                  parse-xhr-response)
         _ (reduce-kv (fn [_ param value]
                        (.setParameterValue uri param value))
                      nil params)
         headers (and headers (to-js headers))
         rp (callback-read-port (fn [result]
                                  (XhrIo/send uri (comp result parser)
                                              (as-str method) body headers)))]
     (if auto-refresh
       rp (promise rp)))))

(defn urlenc-params
  "Encode a map into a form-params string"
  [params]
  (join \&
        (map #(str (gstr/urlEncode (key %))
                   \=
                   (gstr/urlEncode (val %)))
             params)))

(defn urldec-params
  "Decode a form-params string into a map"
  [s]
  (apply hash-map
         (map gstr/urlDecode
              (mapcat #(split % #"=")
                      (split s #"&")))))

(defn dom-clone!
  "Clone dom nodes. This draws on the usual convention to represent
  markup as a node or collection of nodes"
  [node-or-nodes]
  (if (sequential? node-or-nodes)
    (map #(.cloneNode % true) node-or-nodes)
    (.cloneNode node-or-nodes true)))

(def random-uuid cljs.core/random-uuid)

(comment
  (declare random-uuid)

  (let [rex (js/RegExp. "[018]" "g")
        replacer #(.toString (bit-xor % (bit-shift-right
                                         (* 16 (js/Math.random))
                                         (/ % 4)))
                             16)
        ;; short for "10000000-1000-4000-8000-100000000000"
        init-string (str 1e7 -1e3 -4e3 -8e3 -1e11)]
    (defn random-uuid "Return a random UUID as string"
      [] (.replace init-string rex replacer))))

(defn save-as [name file]
  (js/saveAs name file))

(defn make-js-zip []
  (js/JSZip.))
