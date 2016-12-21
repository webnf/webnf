(ns webnf.cats.connie
  "CPS library"
  #?(:cljs (:require-macros [webnf.cats.connie :refer [defcontinuation]])))

(defprotocol Continuation
  (continue* [c final]))

#?
(:clj
 (do
   (defn self-sym [form]
     (or (:self-as (meta form))
         (gensym "_")))

   (defmacro cont* [k & body]
     `(reify Continuation
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
        Continuation
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

#_(def id
    (fn
      ([] (cont* k (k)))
      ([a] (cont* k (k a)))
      ([a b] (cont* k (k a b)))
      ([a b c] (cont* k (k a b c)))
      ([a b c & ds] (cont* k (apply k a b c ds)))))


(comment

  ((-> (ccomp (fn [a b]
                (cont* k (k (* a b) (/ a b) (/ b a))))
              (fn [b c]
                (cont* k (k (+ b c) (- b c)))))
       run-cont)
   1 3)

  )
