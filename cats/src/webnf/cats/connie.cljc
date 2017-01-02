(ns webnf.cats.connie
  "CPS library"
  #?(:cljs (:require-macros [webnf.cats.connie :refer [cont* defcontinuation defcontinuation*]])))

(defprotocol Thunk
  "Suspended computation"
  (continue* [thunk continuation]
    "Gets passed a continuation for passing result of thunk.
     Generally returns result of applying continuation to thunk result.
     The main purpose of this abstraction is to allow a concept of multiple return values."))

#?
(:clj
 (do
   (defn self-sym [form]
     (or (:self-as (meta form))
         (gensym "_")))

   (defmacro cont* [k & body]
     `(reify Thunk
        (continue* [~(self-sym k) ~k]
          ~@body)))

   (defmacro cont-fn [& args]
     (let [[fn-name [k & args :as argt] & body]
           (if (symbol? (first args))
             args (cons (gensym "cont-") args))]
       `(fn ~fn-name ~(vec args)
          (cont* ~k ~@body))))

   (defmacro fn-cont [& args]
     (let [[fn-name [k & args :as argt] & body]
           (if (string? (first args))
             args (cons (gensym "cont-") args))]
       `(cont* ~k (fn ~fn-name ~(vec args)
                    ~@body))))

   (defmacro defcontinuation* [rec-name [k & args :as argt] & body]
     `(defrecord ~rec-name ~(vec args)
        Thunk
        (continue* [~(self-sym argt)
                    ~k]
          ~@body)))

   (defmacro defcontinuation [rec-name cons-name args & body]
     `(do
        (declare ~cons-name)
        (defcontinuation* ~rec-name ~args ~@body)
        (defn ~cons-name ~(vec (next args))
          (~(symbol (str "->" rec-name)) ~@(next args)))))))

(defn run-cont*
  ([m] (run-cont* m identity))
  ([m f] (continue* m f)))

(defn run-cont
  ([m] (run-cont* m vector))
  ([m f] (run-cont* m f)))

(defcontinuation Exit exit [_ final-value]
  final-value)

(def get-cc (cont* cc (cc cc)))

(defn cont** [fk]
  (cont* k (fk k)))

(defn map-cont* [f c]
  (cont* k (f (run-cont* c k))))

(defn with-cont* [fk c]
  (cont* k (run-cont* c (fk k))))

(defn cfn [k f]
  (comp #(continue* % k) f))

(defn map-cont [fc c]
  #_(cont* k (run-cont* (run-cont* c fc) k))
  (run-cont* c fc))

(defcontinuation Comp ccomp [k cf cg]
  (cfn (cfn k cf) cg))

(defcontinuation* Pure1 [k v1] (k v1))
(defcontinuation* Pure2 [k v1 v2] (k v1 v2))
(defcontinuation* Pure3 [k v1 v2 v3] (k v1 v2 v3))
(defcontinuation* Pure  [k v1 v2 v3 vals] (apply k v1 v2 v3 vals))

(defn pure
  "Pass args to continuation"
  ([] (cont* k (k)))
  ([a] (->Pure1 a))
  ([a b] (->Pure2 a b))
  ([a b c] (->Pure3 a b c))
  ([a b c & ds] (->Pure a b c ds)))

(defn cfmap
  "Like (continuation) fmap, but map a thunk-returning function."
  [c cf]
  (continue* c cf))

(defn join
  "(continuation) monadic join"
  [cc]
  (cont* k
         (continue* cc #(continue* % k))))

(defn run-m
  "Run monad stack based on the continuation monad"
  ([cm final]
   (continue* (cm pure cfmap join) final))
  ([cm ts final] (run-m (ts cm) final)))
