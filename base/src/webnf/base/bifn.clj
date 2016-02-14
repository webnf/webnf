(ns webnf.base.bifn
  "WARNING: Experimental")

(defprotocol BiFn
  (to-in  [f  x] "Regular direction, should map to (f x), if appropriate")
  (to-out [rf y] "Reverse direction"))

(defmacro bifn [to-in-impl to-out-impl]
  `(reify
     BiFn
     ~(cons 'to-in to-in-impl)
     ~(cons 'to-out to-out-impl)))

(def identity-df
  (bifn
   ([_ out] out)
   ([_ in] in)))

(defn df-comp
  ([] identity)
  ([f] f)
  ([f g]
   (bifn
    ([_ out] (to-in f (to-in g out)))
    ([_ in] (to-out g (to-out f in)))))
  ([f g & rst]
   (let [spl (/ (max 0 (- (count rst)
                          2))
                2)]
     (comp
      (apply comp f g (take spl rst))
      (apply comp (drop spl rst))))))

(defn df-map
  ([m]
   (bifn
    ([_ out]
     (persistent!
      (reduce-kv
       (fn [acc k df]
         (if (contains? out k)
           (assoc! acc k (to-in df (get out k)))
           acc))
       (transient (empty out))
       m))))))

(do                                     ; testing
  (defn incer [n]
    (bifn ([_ x] (+ x n))
          ([_ x] (- x n))))
  (defn muller [n]
    (bifn ([_ x] (* x n))
          ([_ x] (/ x n))))
  (def im (comp (muller 5) (incer 3))))
