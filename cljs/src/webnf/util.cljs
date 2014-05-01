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
   [webnf.promise :refer [promise]])
  (:require-macros 
   [cljs.core.async.macros :refer [go]]))

(defn log 
  "Log args directly to the browser console"
  [& args]
  (when-let [con js/console]
    (.apply (.-log con) con (to-array args))))

(defn log-pr 
  "Log pr-str of args to browser console"
  [& args]
  (apply log (map pr-str args)))

(when-not *print-fn* ;; TODO: make this configurable, somehow?
  (set! *print-fn* log-pr))

(defn scat
  "Returns a function taking a seq on which f is applied.
   To [scat]ter is an antonym of to [juxt]apose."
  [f]
  #(apply f %1))

(def to-js
  "Makes a js object from a map"
  (comp (scat js-obj) (scat concat)))

;; ### jQuery helper functions
;;
;; These functions just let you pass in strings to interact with
;; jQuery and other plain javascript method calls without externs.
;; The closure compiler will eliminate double string literals, so we
;; can get away with this.

(defn $a*
  "Apply window.jQuery to a js array of args"
  [arrgs]
  (.apply (aget js/window "jQuery")
          nil
          arrgs))

(defn $* 
  "Apply window.jQuery to args"
  [& args]
  ($a* (to-array args)))

(defn $a- 
  "Call string method on obj with array of args"
  [obj meth arrgs]
  (and obj
       (.apply (aget obj meth) obj arrgs)))

(defn $- 
  "Call string method on obj with args"
  [obj meth & args]
  ($a- obj meth (to-array args)))

(defn $$ 
  "Call window.jQuery.<method> with args"
  [meth & args]
  ($a- (aget js/window "jQuery") meth (to-array args)))

(defn $
  "Call window.jQuery(obj).<method> with args"
  ([obj] ((aget js/window "jQuery") obj))
  ([obj meth & args]
     ($a- ($ obj) meth (to-array args))))

(defn $?
  "jQuery predicate: array with length > 0"
  [jq] (and jq (pos? (alength jq))))

;; ## Async load infrastructure

(defn eval-js
  "Evaluate javascript string by adding a script tag to the document head"
  [thunk]
  (.appendChild 
   (.-head js/document)
   (dom/createDom "script" nil thunk)))

(defn add-stylesheets
  "Add a links to style sheets to the document head"
  [& hrefs]
  (doseq [href hrefs]
    (dom/appendChild (.-head js/document)
                     (dom/createDom "link" (js-obj "href" href
                                                   "rel" "stylesheet")))))

;; Here is some infrastructure to load up javascript asynchronously,
;; but still keep the order of evaluation for dynamically loading
;; dependencies with subdependencies and interleave javascript loaded
;; from a server with javascript written inline.

(defn- load-init-fetch [fetched-atom after-load scripts]
  (doseq [src scripts]
    (XhrIo/send src (fn [e]
                      (let [xhr (.-target e)]
                        (when (= NET/COMPLETE (.-type e))
                          (swap! fetched-atom assoc
                                 src
                                 (.getResponseText xhr))
                          (after-load)))))))

(defn take-completed [todo-steps fetched]
  (loop [[[op data] :as steps] todo-steps
         result {:thunks []}]
    (if-not (seq steps)
      (assoc result :todo steps)
      (case op
        :eval (recur (rest steps) 
                     (update-in result [:thunks] 
                                conj data))
        :src  (if-let [t (fetched data)]
                (recur (rest steps)
                       (update-in result [:thunks]
                                  conj t))
                (assoc result :todo steps))))))

(defn queued-load [& type-steps]
  (let [q (partition 2 type-steps)
        fetched-atom (atom {})
        todo-atom (atom q)
        after-load (fn after-load []
                     (let [{:keys [thunks todo]} (take-completed @todo-atom @fetched-atom)]
                       (doseq [thunk thunks]
                         (eval-js thunk))
                       (reset! todo-atom todo)))]
    (load-init-fetch fetched-atom after-load
                     (map second (filter (comp #(= :src %) first)
                                         q)))
    (after-load)))


;; ### XHR

;; why is this not in gclosure?

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
    {:uri uri
     :status (.getStatus t)
     :headers (hmap (.getAllResponseHeaders t))
     :body (.getResponseText t)}))

(defn xhr
  "Request an uri with XmlHttpRequest, return channel that will
  receive response in ring format {:uri :status :headers :body}

  Request data can also be passed in ring format
  {:method :params :headers :body :always-refresh}
  :auto-refresh true triggers a request on every read"
  ([uri] (xhr uri nil))
  ([uri {:keys [method body headers params auto-refresh]}]
     (let [uri (Uri/parse uri)
           _ (reduce-kv (fn [_ param value]
                          (.setParameterValue uri param value))
                        nil params)
           headers (and headers (to-js headers))
           rp (callback-read-port (fn [result]
                                    (XhrIo/send uri (comp result parse-xhr-response)
                                                method body headers)))]
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
