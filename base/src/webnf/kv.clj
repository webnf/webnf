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
                  (transient (or init (if kvr {} []))) coll))))

(defn map-to-kv
  "Maps (f assoc! v) over a coll to build up a map
   f should return the result of (assoc! k* v*)"
  [f coll]
  (persistent!
   (reduce (fn [t v]
             (f #(assoc! t %1 %2) v))
           (transient {}) coll)))

(defn map-kv
  "Maps (f assoc! k v) over a map, where
   [k v] are MapEntry pairs from coll.
   f should return the result of (assoc! k* v*)"
  [f coll]
  (treduce-kv (fn [t k v]
                (f #(assoc-kv! t %1 %2) k v))
              (empty coll) coll))

(defn map-juxt
  "Map separate key and value functions over seq to build up a map"
  [fk fv coll]
  (map-to-kv #(%1 (fk %2) (fv %2)) coll))

(defn map-juxt-kv
  "Map separate key and value functions over keys and vals of MapEntry coll"
  [fk fv coll]
  (map-kv #(%1 (fk %2) (fv %3))
          coll))

(defn map-keys 
  "Map a function over the keys of MapEntry coll"
  [f coll]
  (map-juxt-kv f identity coll))

(defn map-vals 
  "Map a function over the vals of MapEntry coll"
  [f coll]
  (map-juxt-kv identity f coll))

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

(let [nnil? (fn [k v] (not (nil? v)))]
  (defn assoc-when
    "Assoc only values matching predicate. Defaults to 'value non-nil'"
    {:arglists (list '[m & {:as kvs}]
                     '[kv-pred? m & {:as kvs}])}
    [& args]
    (let [[kv-pred? m kvs] (if (even? (count args))
                             [(first args) (second args) (nnext args)]
                             [nnil? (first args) (next args)])]
      (loop [tm (transient m)
             [k v & rst :as kvs*] kvs]
        (if (seq kvs*)
          (recur (if (kv-pred? k v)
                   (assoc! tm k v)
                   tm)
                 rst)
          (persistent! tm))))))

(defn- assoc-when-arm [p-s k-ex v-ex]
  `(as-> trans# (let [k# ~k-ex]
                  (if (~p-s k#)
                    (let [v# ~v-ex]
                      (if (~p-s k# v#)
                        (assoc! trans# k# v#)
                        trans#))
                    trans#))))

(let [nnil? `(fn* ([k#] true) ([k# v#] (not (nil? v#))))]
  (defmacro assoc-when*
    "Calls (kv-pred key) for every key to be assoced. If true, value is evaluated and (kv-pred key val) is called. If true, assoc."
    {:arglists '([m & {:as kvs}] [kv-pred? m & {:as kvs}])}
    [& args]
    (let [[kv-pred-expr m-expr kv-exprs] (if (even? (count args))
                                           [(first args) (second args) (nnext args)]
                                           [nnil? (first args) (next args)])
          pred-s (gensym "pred-")]
      `(let [~pred-s ~kv-pred-expr]
         (-> (transient ~m-expr)
             ~@(for [[kex vex] (partition 2 kv-exprs)]
                 (assoc-when-arm pred-s kex vex))
             persistent!)))))
