(ns webnf.base.utils
  (:require [webnf.base.autoload :refer [autoload]]))

(defn to-many
  "Ensure that seq can be called on a value. If value is not a coll
  and not nil, it is put into an empty collection"
  [v]
  (if (or (nil? v) (coll? v)) v (cons v nil)))

(defmacro squelch 
  "Eval body with a handler for Exception that returns a default expression val.
  Logs exceptions on trace priority."
  [val & body]
  `(try ~@body (catch Exception e#
                 (let [val# ~val]
                   (log/trace e# "during execution of" 
                              (pprint-str '(try ~@body (catch Exception e ...)))
                              "\n used replacement value:" val#)
                   val#))))

(autoload clojure.pprint/pprint)

(defn pprint-str
  "Return value pretty-printed into a string.
   Allows for clojure.pprint/*print-right-margin* to be passed as second argument"
  ([o] (with-out-str (pprint o)))
  ([o right-margin]
     (require 'clojure.pprint)
     (binding [clojure.pprint/*print-right-margin* right-margin]
       (pprint-str o))))

(defmacro forcat
  "Concat the return value of a for expression"
  [bindings body]
  `(apply concat (for ~bindings ~body)))

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
  (reduce #(%2 %1) x fs))

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
  ([x & fs] (reduce #(list %2 %1) x fs)))

(defunrolled rcomp
  :more-arities ([args] `(apply rcomp* ~args))
  ([& fs] `(pretial ap ~@fs)))
