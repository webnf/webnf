(ns webnf.connie
  "CPS library")

(defprotocol Continuation
  (continue* [c final]))

(defmacro cont* [k & body]
  `(reify Continuation
     (continue* [_ ~k]
       ~@body)))

(defn run-cont*
  ([m] (run-cont* m identity))
  ([m f] (continue* m f)))

(defn run-cont
  ([m] (run-cont* m vector))
  ([m f] (run-cont* m f)))

(defmacro defcontinuation* [rec-name [k & args] & body]
  `(defrecord ~rec-name ~(vec args)
     Continuation
     (continue* [_# ~k]
       ~@body)))

(defmacro defcontinuation [rec-name cons-name args & body]
  `(do (defcontinuation* ~rec-name ~args ~@body)
       (defn ~cons-name ~(vec (next args))
         (~(symbol (str "->" rec-name)) ~@(next args)))))

(defcontinuation Exit exit [_ final-value]
  final-value)
