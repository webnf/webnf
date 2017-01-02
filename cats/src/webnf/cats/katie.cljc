(ns webnf.cats.katie
  "Top-Level Categories:
    - Applicative (pure, fmap)"
  (:require [webnf.cats.connie :as c :refer
             [Thunk cont* defcontinuation defcontinuation* continue*]]))

(defn pure [& vals]
  (fn [pure _ _]
    (apply pure vals)))

(defn cfmap [m cf]
  (fn [pure cfmap join]
    (cfmap (m pure cfmap join)
           cf)))

(defn fmap [m f]
  (cfmap m (fn [& vals] (cont* k (k (apply f vals))))))


(defcontinuation* SeqAp [k mf ma]
  (continue* mf (fn [f] (continue* ma (comp k f)))))

(defn <*>
  ([mf ma]       (fn [pure cfmap join] (->SeqAp (mf pure cfmap join)
                                                (ma pure cfmap join))))
  ([mf ma & mbs] (apply <*> (<*> mf ma) mbs)))

(defcontinuation* FMap [k f ma]
  (continue* ma (comp k f)))

(defn <$>
  ([f ma]       (fn [pure cfmap join] (->FMap f (ma pure cfmap join))))
  ([f ma & mbs] (apply <*> (<$> f ma) mbs)))

(defcontinuation* Ap2 [k m1 m2] (continue*
                                 m1 (fn apv-1 [v1] (continue*
                                                    m2 (fn apv2 [v2]
                                                         (k v1 v2))))))
(defcontinuation* Apc [k m1 m2] (continue*
                                 m1 (fn [v1] (continue*
                                              m2 (partial k v1)))))

(defn <>
  ([] (fn [_ _ _] (reify Thunk (continue* [_ k] (k)))))
  ([m1] m1)
  ([m1 m2] (fn [pure cfmap join]
             (->Ap2 (m1 pure cfmap join)
                    (m2 pure cfmap join))))
  ([m1 m2 & ms] (fn [pure cfmap join]
                  (->Apc (m1 pure cfmap join)
                         ((apply <> m2 ms)
                          pure cfmap join)))))

(defcontinuation* SeqRight [k ma mb]
  (continue* ma (fn [& _] (continue* mb k))))

(defcontinuation* SeqLeft [k ma mb]
  (continue* ma (fn [& v] (continue* mb (fn [& _] (apply k v))))))

(defn *>
  ([m1 m2] (fn [pure cfmap join] (->SeqRight (m1 pure cfmap join)
                                             (m2 pure cfmap join))))
  ([m1 m2 & ms] (*> m1 (apply *> m2 ms))))

(defn <*
  ([m1 m2] (fn [pure cfmap join]
             (->SeqLeft (m1 pure cfmap join)
                        (m2 pure cfmap join))))
  ([m1 m2 & ms] (apply <* (<* m1 m2) ms)))

