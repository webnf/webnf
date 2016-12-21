(ns webnf.cats.katie
  "Top-Level Categories:
    - Applicative (pure, fmap)"
  (:require [webnf.cats.connie :refer
             [Continuation cont* defcontinuation defcontinuation* continue*]]))

(defcontinuation* Pure1 [k v1] (k v1))
(defcontinuation* Pure2 [k v1 v2] (k v1 v2))
(defcontinuation* Pure3 [k v1 v2 v3] (k v1 v2 v3))
(defcontinuation* Pure  [k args] (apply k args))

(defn- third [c] (nth c 2))

(defn pure [& v]
  (case (count v)
    0 (cont* k (k))
    1 (->Pure1 (first v))
    2 (->Pure2 (first v) (second v))
    3 (->Pure3 (first v) (second v) (third v))
    (->Pure (vec v))))

(defcontinuation* SeqAp [k mf ma]
  (continue* mf (fn [f] (continue* ma (comp k f)))))

(defn <*>
  ([mf ma]       (->SeqAp mf ma))
  ([mf ma & mbs] (apply <*> (->SeqAp mf ma) mbs)))

(defcontinuation* FMap [k f ma]
  (continue* ma (comp k f)))

(defn <$>
  ([f ma]       (->FMap f ma))
  ([f ma & mbs] (apply <*> (<$> f ma) mbs)))

(defcontinuation* Ap2 [k m1 m2] (continue*
                                 m1 (fn apv-1 [v1] (continue*
                                                    m2 (fn apv2 [v2]
                                                         (k v1 v2))))))
(defcontinuation* Apc [k m1 m2] (continue*
                                 m1 (fn [v1] (continue*
                                              m2 (partial k v1)))))

(defn <>
  ([] (reify Continuation (continue* [_ k] (k))))
  ([m1] m1)
  ([m1 m2] (->Ap2 m1 m2))
  ([m1 m2 & ms] (->Apc m1 (apply <> m2 ms))))

(defcontinuation* SeqRight [k ma mb]
  (continue* ma (fn [& _] (continue* mb k))))

(defcontinuation* SeqLeft [k ma mb]
  (continue* ma (fn [& v] (continue* mb (fn [& _] (apply k v))))))

(defn *>
  ([m1 m2] (->SeqRight m1 m2))
  ([m1 m2 & ms] (*> m1 (apply *> m2 ms))))

(defn <*
  ([m1 m2] (->SeqLeft m1 m2))
  ([m1 m2 & ms] (apply <* (<* m1 m2) ms)))

