(ns webnf.cats.monie
  "CPS based monads"
  (:require [webnf.cats.katie :refer [pure *> fmap]]
            [webnf.cats.connie :as c :refer [cont* defcontinuation* defcontinuation continue* cfn run-m]]))

(def return pure)
(def >> *>)

(defn join [mm]
  (fn [pure cfmap join]
    (join (mm pure cfmap join))))

(defn >>= [c f]
  #_(fn [pure cfmap join]
      (join (cfmap (c pure fmap join)
                   (fn [& vals]
                     ((apply f vals)
                      pure fmap join)))))
  (join (fmap c f)))

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


(comment
  (->
   (mdo [[x y] (pure 1 2)]
        (pure (+ x y)))
   (run-m vector))

  (defn either-t [m right right?]
    (fn [base-pure base-cfmap base-join]
      (letfn [(either-pure [& vals]
                (base-pure (apply right vals)))
              (either-cfmap [em cf]
                (base-cfmap
                 em (fn [& vals]
                      (if (apply right? vals)
                        (continue* (apply cf vals) right)
                        (apply c/pure vals)))))
              (either-join [eem]
                (base-cfmap eem identity))]
        (m either-pure either-cfmap either-join))))
  
  (-> (pure 1)
      (fmap inc)
      (either-t #(c/pure (* % 2)) #(zero? (mod % 2)))
      (run-m vector))
  

  )
