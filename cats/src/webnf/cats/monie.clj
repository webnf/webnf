(ns webnf.cats.monie
  "CPS based monads"
  (:require [webnf.cats.katie :refer [pure *>]]
            [webnf.cats.connie :refer [cont* defcontinuation* defcontinuation continue*]]))

(comment
  ;; IFn as Continuation
 (defn >>=
   "Monie's binding descriptor. Here she gets aquainted with a coworkers input continuation and
   returns a version of herself with her output connected to it."
   [m f]
   (fn [k]
     (m (fn ret [& v]
          ((apply f v) k)))))

 (defn return
   "Returns a neutral monie"
   [& v]
   (fn [k] (apply k v)))

 (defmacro mdo* [continue-from [binding step & nxt :as bindings] & body]
   {:pre [(zero? (mod (count bindings) 2))]}
   (if (seq bindings)
     `(>>= (~continue-from ~step) (fn ~binding (mdo* ~continue-from ~nxt ~@body)))
     `(do ~@body)))

 (defmacro mdo [bindings & body]
   `(mdo* identity ~bindings ~@body)))

(def return pure)
(def >> *>)

(defn- cfn [f k]
  (fn
    ([] (continue* (f) k))
    ([a] (continue* (f a) k))
    ([a b] (continue* (f a b) k))
    ([a b c] (continue* (f a b c) k))
    ([a b c & ds] (continue* (apply f a b c ds) k))))

(defcontinuation* Binding [k ctx recv]
  (continue* ctx (cfn recv k)))

(defn >>= [c f]
  (->Binding c f))

(defmacro mdo [[binding step & nxt :as bindings] & body]
  {:pre [(zero? (mod (count bindings) 2))]}
  (if (seq bindings)
    (if (= '_ binding)
      `(>> ~step (mdo ~nxt ~@body))
      `(>>= ~step (fn ~binding (mdo ~nxt ~@body))))
    `(do ~@body)))

(comment

  (defprotocol MBound
    (m-bound [m k ret] "Monie's conversation operation.
     She gets passed a continuation [k], to which she passes her own continuation [ret]."))

  (defn >>=
    "Monie's binding descriptor. Here she gets aquainted with a coworkers input continuation and
   returns a version of herself with her output connected to it."
    [m f]
    (fn [k]
      (m-bound m k (fn ret [& v]
                     ((apply f v) k)))))

  (defn return
    "Returns a neutral Monie"
    [& v]
    (reify MBound
      (m-bound [_ k ret]
        (apply ret v))))

  (defn run-io [m]
    (m vector)))
