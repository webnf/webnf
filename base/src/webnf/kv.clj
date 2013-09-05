(ns webnf.kv)

(defprotocol TKV
  (assoc-kv! [coll k v]))

(extend-protocol TKV
  clojure.lang.ITransientMap
  (assoc-kv! [m k v] (assoc! m k v))
  clojure.lang.ITransientCollection
  (assoc-kv! [c k v] (conj! c (clojure.lang.MapEntry. k v))))

(defn treduce-kv [f init s]
  (let [kvr (map? s)]
    (persistent! ((if kvr 
                    reduce-kv
                    reduce) 
                  (if kvr
                    f
                    #(let [[k v] %2] (f %1 k v)))
                  (transient init) s))))

(defn map-kv [f s]
  (treduce-kv (fn [t k v]
                (f #(assoc-kv! t %1 %2) k v))
              (empty s) s))

(defn map-juxt [fk fv m]
  (map-kv #(%1 (fk %2) (fv %3))
          m))

(defn map-keys [f m]
  (map-juxt f identity m))

(defn map-vals [f m]
  (map-juxt identity f m))

(defn apply-kw
  "Like apply, but f takes keyword arguments and the last argument is
  not a seq but a map with the arguments for f"
  [f & args]
  {:pre [(map? (last args))]}
  (apply f (apply concat
                  (butlast args) (last args))))

(defn merge-deep [m & maps]
  (persistent!
   (reduce
    (fn [merged m]
      (reduce-kv (fn [merged k v]
                   (assoc! merged k
                           (if-let [ov (get merged k)]
                             (if (and (map? ov) (map? v))
                               (merge-deep ov v)
                               v)
                             v)))
                 merged m))
    (transient m) maps)))
