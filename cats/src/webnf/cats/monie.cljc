(ns webnf.cats.monie
  "CPS based monads"
  (:require [webnf.cats.katie :refer [pure *>]]
            [webnf.cats.connie :refer [cont* defcontinuation* defcontinuation continue* cfn]]))

(def return pure)
(def >> *>)

(defcontinuation* Binding [k ctx recv]
  (continue* ctx (cfn recv k)))

(defn >>= [c f]
  (->Binding c f))

(def id
  (fn
    ([] (cont* k (k)))
    ([a] (cont* k (k a)))
    ([a b] (cont* k (k a b)))
    ([a b c] (cont* k (k a b c)))
    ([a b c & ds] (cont* k (apply k a b c ds)))))

#?
(:clj
 (do
   (defmacro mdo [[binding step & nxt :as bindings] & body]
     {:pre [(zero? (mod (count bindings) 2))]}
     (if (seq bindings)
       (if (= '_ binding)
         `(>> ~step (mdo ~nxt ~@body))
         `(>>= ~step (fn ~binding (mdo ~nxt ~@body))))
       `(do ~@body)))))
