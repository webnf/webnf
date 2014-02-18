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
   [cljs.core.async :refer [chan >! close!]])
  (:require-macros 
   [cljs.core.async.macros :refer [go]]))

(defn log [& args]
  (when-let [con js/console]
    (.apply (.-log con) con (to-array args))))

(defn log-pr [& args]
  (apply log (map pr-str args)))

(set! *print-fn* log-pr)

(defn scat
  "Returns a function taking a seq on which f is applied.
   To [scat]ter is an antonym of to [juxt]apose."
  [f]
  #(apply f %1))

(def to-js
  "Makes a js object from a map"
  (comp (scat js-obj) (scat concat)))

(defn $a* [arrgs]
  (.apply (aget js/window "jQuery")
          nil
          arrgs))

(defn $* [& args]
  ($a* (to-array args)))

(defn $a- [obj meth arrgs]
  (and obj
       (.apply (aget obj meth) obj arrgs)))

(defn $- [obj meth & args]
  ($a- obj meth (to-array args)))

(defn $$ [meth & args]
  ($a- (aget js/window "jQuery") meth (to-array args)))

(defn $
  ([obj] ((aget js/window "jQuery") obj))
  ([obj meth & args]
     ($a- ($ obj) meth (to-array args))))

(defn $? [jq] (and jq (pos? (alength jq))))

(defn eval-js [thunk]
  (.appendChild 
   (.-head js/document)
   (dom/createDom "script" nil thunk)))

(defn add-stylesheets [& hrefs]
  (doseq [href hrefs]
    (dom/appendChild (.-head js/document)
                     (dom/createDom "link" (js-obj "href" href
                                                   "rel" "stylesheet")))))

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

(defn hmap [hs]
  (persistent!
   (reduce (fn [res l]
             (if-let [[_ k v] (re-matches #"([^:]*):(.*)" l)]
               (assoc! res (.trim k) (.trim v))
               (do
                 (log "ERROR Not a header" l)
                 res)))
           (transient {}) (split-lines hs))))

(defn xhr 
  ([uri] (xhr uri nil))
  ([uri {:keys [method body headers params]}]
     (let [ch (chan)
           uri (Uri/parse uri)]
       (reduce-kv (fn [_ param value]
                    (.setParameterValue uri param value))
                  nil params)
       (XhrIo/send uri #(let [t (.-target %)
                              rt {:uri uri
                                  :status (.getStatus t)
                                  :headers (hmap (.getAllResponseHeaders t))
                                  :body (.getResponseText t)}]
                          (go (>! ch rt)
                              (close! ch)))
                   method
                   body
                   (and headers (to-js headers)))
       ch)))

(defn urlenc-params [params]
  (join \&
        (map #(str (gstr/urlEncode (key %))
                   \=
                   (gstr/urlEncode (val %)))
             params)))

(defn urldec-params [s]
  (apply hash-map
         (map gstr/urlDecode
              (mapcat #(split % #"=")
                      (split s #"&")))))

(defn dom-clone! [node-or-nodes]
  (if (sequential? node-or-nodes)
    (map #(.cloneNode % true) node-or-nodes)
    (.cloneNode node-or-nodes true)))
