(ns webnf.kv
  "Functions to efficiently handle associatives with reducers and transients.")

(defprotocol TKV
  "Protocol to build up a transient collection of MapEntries"
  (assoc-kv! [coll k v]))

(extend-protocol TKV
  clojure.lang.ITransientMap
  (assoc-kv! [m k v] (assoc! m k v))
  clojure.lang.ITransientCollection
  (assoc-kv! [c k v] (conj! c (clojure.lang.MapEntry. k v))))

(defn treduce-kv 
  "use reduce(-kv) over collection of map-entries for building up a transient of init.
  Calls (f tr key val) with [key val] from coll.
  Use higher level operations like map-keys, map-vals, map-kv, map-juxt where possible"
  [f init coll]
  (let [kvr (map? coll)]
    (persistent! ((if kvr 
                    reduce-kv
                    reduce) 
                  (if kvr
                    f
                    #(let [[k v] %2] (f %1 k v)))
                  (transient init) coll))))

(defn map-kv
  "Maps (f assoc! k v) over a map, where
   [k v] are MapEntry pairs from coll.
   f should return the result of (assoc! k* v*)"
  [f coll]
  (treduce-kv (fn [t k v]
                (f #(assoc-kv! t %1 %2) k v))
              (empty coll) coll))

(defn map-juxt
  "Map separate key and value functions over keys and vals of MapEntry coll"
  [fk fv coll]
  (map-kv #(%1 (fk %2) (fv %3))
          coll))

(defn map-keys 
  "Map a function over the keys of MapEntry coll"
  [f coll]
  (map-juxt f identity coll))

(defn map-vals 
  "Map a function over the vals of MapEntry coll"
  [f coll]
  (map-juxt identity f coll))

(defn apply-kw
  "Like apply, but f takes keyword arguments and the last argument is
  not a seq but a map with the arguments for f"
  [f & args]
  {:pre [(map? (last args))]}
  (apply f (apply concat
                  (butlast args) (last args))))

(defn merge-deep 
  "Deep merge with transients and reduce.
  On merge collisions, if both values are maps, they are deep-merged.
  Otherwise, the right entry wins."
  [m & maps]
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
