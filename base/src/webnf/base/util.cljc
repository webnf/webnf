(ns webnf.base.util
  (:refer-clojure :exclude [update-in])
  #?(:cljs (:require-macros [webnf.base.util :refer [defunrolled or*]]))
  (:require
   [clojure.string :as str]
   #?@(:clj  [[webnf.base.autoload :refer [autoload]]
              [webnf.base.cljc :refer [defmacro*]]
              [clojure.tools.logging :as log]]
       :cljs [[cljs.pprint :refer [pprint]]
              [webnf.base.logging :as log :include-macros true]]))
  #?(:clj  (:import (java.net URLEncoder URLDecoder))
     :cljs (:import (goog.string StringBuffer))))

(defn update-in
  "Version of {clojure,cljs}.core/update-in, fixed for empty paths
  see http://dev.clojure.org/jira/browse/CLJ-1623"
  [m ks f & args]
  (if-let [[k & ks*] (seq ks)]
    (assoc m k (apply update-in (get m k) ks* f args))
    (apply f m args)))

#?(:clj (autoload clojure.pprint/pprint))

(defn pprint-str
  "Return value pretty-printed into a string.
   Allows for clojure.pprint/*print-right-margin* to be passed as second argument"
  ([o] (with-out-str (pprint o)))
  ([o right-margin]
   #?(:clj (require 'clojure.pprint))
   (binding [#?(:clj  clojure.pprint/*print-right-margin*
                :cljs cljs.pprint/*print-right-margin*)
             right-margin]
     (pprint-str o))))

#?
(:clj
 (do
   (defmacro* squelch
     "Eval body with a handler for Exception that returns a default expression val.
  Logs exceptions on trace priority."
     [val & body]
     :clj
     `(try ~@body (catch Exception e#
                    (let [val# ~val]
                      (log/trace e# "during execution of"
                                 ~(pprint-str `(try ~@body ~'(catch Exception e ...)))
                                 "\n used replacement value:" val#)
                      val#)))
     :cljs
     `(try ~@body (catch js/Error e#
                    (let [val# ~val]
                      (webnf.base.logging/trace e# "during execution of"
                                                ~(pprint-str `(try ~@body ~'(catch js/Error e ...)))
                                                "\n used replacement value:" val#)
                      val#))))
   (defmacro* forcat
     "Concat the return value of a for expression"
     [bindings body]
     :clj  `(apply concat (for ~bindings ~body))
     :cljs `(cljs.core/apply cljs.core/concat (cljs.core/for ~bindings ~body)))

   (defmacro static-case
     "Variant of case where keys are evaluated at compile-time
   WARNING: only use this for dispatch values with stable hashes,
     like edn literals, java Enums, ..."
     [val & cases]
     `(case ~val
        ~@(forcat [[field thunk] (partition 2 cases)]
                  [(eval field) thunk])
        ~@(when (odd? (count cases))
            [(last cases)])))

   (defmacro defunrolled
     "Ever get that itch to unroll vararg functions for performance?
   Usage:
    (defunrolled comp*
      :unroll 3
      :more-arities ([args] `(apply comp* (for [as# (partition-all 3 ~args)]
                                            (apply comp* as#))))
      ([] identity)
      ([f] f)
      ([f & fs] (let [arg (gensym \"arg-\")]
                  `(fn* [~arg] ~(reduce #(list %2 %1) arg (reverse (cons f fs)))))))
   "
     [self & flags+arities]
     (let [{:keys [min unroll more-arities doc gen-fn]
            :or {min 0 unroll 8}}
           (loop [flags {} [f v :as fas] flags+arities]
             (cond
               (string? f)
               (recur (assoc flags :doc f) (next fas))
               (keyword? f)
               (recur (assoc flags f v) (nnext fas))
               :else
               (assoc flags :gen-fn (eval (cons 'fn* fas)))))
           fixed (vec (repeatedly min #(gensym "a-")))
           vars (repeatedly unroll #(gensym "v-"))]
       `(defn ~self ~@(when doc [doc])
          ~@(for [n-v (range (inc unroll))
                  :let [args (into fixed (take n-v vars))]]
              (list args (apply gen-fn args)))
          ~@(when-let [gen (and more-arities
                                (eval (cons 'fn* more-arities)))]
              (let [vararg (gensym "va-")
                    args (into fixed vars)]
                [(list (into args ['& vararg])
                       `(let [~vararg (list* ~@args ~vararg)]
                          ~(gen vararg)))])))))

   (defmacro or* "Variant of or, that only skips nils"
     [v d]
     `(let [v# ~v] (if (nil? v#) ~d v#)))))

(defn scat
  "Returns a function taking a seq on which f is applied.
   To [scat]ter is an antonym of to [juxt]apose."
  [f]
  #(apply f %1))

(defn to-many
  "Ensure that seq can be called on a value. If value is not a coll
  and not nil, it is put into an empty collection"
  [v]
  (if (or (nil? v) (coll? v)) v (cons v nil)))

;; ## Two new core operations

;; ### pretial -- partial for the first param

;; This is the main proposition: introduce an operation that lets one
;; partially bind clojure's collection functions

(defn pretial* [f & args]
  (fn [o] (apply f o args)))

;; ### ap -- start chains of pretials

;; This is the entry point for update functions
;; it's like a reverse comp, that fits in the update-fn slot:
;; (update-in x [y z] ap
;;            (pretial assoc :a :b)
;;            (pretial dissoc :c))
(defn ap* [x & fs]
  (reduce #((or* %2 identity) %1) x fs))

;; ### rcomp seems to naturally fall out

(defn rcomp* [& fs]
  (apply pretial* ap* fs))

(defunrolled pretial
  :min 1
  :more-arities ([args] `(apply pretial* ~args))
  ([f] f)
  ([f & args] `(fn* [o#] (~f o# ~@args))))

(defunrolled ap
  :min 1
  :more-arities ([args] `(apply ap* ~args))
  ([x & fs] (reduce  #(list `(or* ~%2 identity) %1) x fs)))

(defunrolled rcomp
  :more-arities ([args] `(apply rcomp* ~args))
  ([& fs] `(pretial ap ~@fs)))

#?
(:clj
 (do (defn encode-uri-component [s]
       (when s
         (URLEncoder/encode s "UTF-8")))
     (defn decode-uri-component [s]
       (when s
         (URLDecoder/decode s "UTF-8"))))
 :cljs
 (do (def encode-uri-component js/encodeURIComponent)
     (def decode-uri-component js/decodeURIComponent)))

(defn path->href
  "Joins path segments (each a seq) into an absolute url path"
  [& path-fragments]
  (str/join "/" (cons "" (map encode-uri-component
                              (apply concat path-fragments)))))

(defn href->path
  "Splits an absolute href into a path segment"
  [href]
  (case href
    ""  (throw (ex-info "Not an absolute path" {:href href}))
    "/" []
    (let [p (map decode-uri-component
                 (str/split (str/replace href "+" " ")
                            #"/"))]
      (when-not (= "" (first p))
        (throw (ex-info "Not an absolute path" {:href href})))
      (vec (rest p)))))

(defn encode-uri-params
  "Encode a map into a form-params string"
  [params]
  (str/join "&"
            (map #(str (encode-uri-component (key %))
                       \=
                       (encode-uri-component (val %)))
                 params)))

(defn decode-uri-params
  "Decode a form-params string into a map"
  [s]
  (apply hash-map
         (map decode-uri-component
              (mapcat #(str/split % #"=" 2)
                      (str/split (str/replace s "+" " ")
                                 #"&")))))

(defn str-quote
  "Quotes string with configurable quote and escape character (default \" and \\)"
  ([v] (str-quote v \" \\))
  ([v q] (str-quote v q \\))
  ([v q e]
   (let [s  (str v)
         s* (-> #?(:clj (eval
                         (if (= q e)
                           `(fn [sb# ch#]
                              (case ch#
                                ~e (.. sb# (append (char ~e)) (append (char ~e)))
                                (.append sb# ch#)))
                           `(fn [sb# ch#]
                              (case ch#
                                ~e (.. sb# (append (char ~e)) (append (char ~e)))
                                ~q (.. sb# (append (char ~e)) (append (char ~q)))
                                (.append sb# ch#)))))
                   :cljs (fn [sb ch]
                           (cond
                             (= ch e) (.. sb (append e) (append e))
                             (= ch q) (.. sb (append e) (append q))
                             :else (.append sb ch))))
                (reduce #?(:clj  (.append (StringBuilder. (+ (count s) 6)) q)
                           :cljs (StringBuffer. q))
                        s)
                (.append q)
                str)]
     (if (= s s*)
       s s*))))
