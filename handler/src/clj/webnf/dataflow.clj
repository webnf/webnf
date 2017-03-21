(ns webnf.dataflow
  "Dataflow framework"
  (:require [clojure.string :as str]))

(defprotocol Dataflow
  (flow [df in-value]))

(defrecord DataflowError [message dataflow cause stack])

(defn error? [value]
  (instance? DataflowError value))

(defn assert-value!
  ([dataflow input-value]
   (assert-value! (flow dataflow input-value)))
  ([value]
   (if (error? value)
     (throw (ex-info (:message value "") value))
     value)))

(defn dataflow-error [error]
  (DataflowError. (:message error "Dataflow Error")
                  (:dataflow error dataflow-error)
                  (:cause error)
                  (:stack error)
                  nil
                  (dissoc error :dataflow :message :cause :stack)))

(defmacro dataflow [[this-param value-param] & body]
  `(reify Dataflow
     (flow [~this-param ~value-param]
       ~@body)))

;; ### Error handling

(defmacro protect-flow
  "Protects forms with try-catch, raising validator errors instead"
  ([body] `(protect-flow {} ~body))
  ([ctx body]
   (let [frm (list 'quote &form)]
     `(let [ctx# ~ctx]
        (try (let [res# ~body]
               (if (error? res#)
                 (update res# :stack conj
                         {:form ~frm})
                 res#))
             (catch Exception e#
               (dataflow-error (merge {:dataflow ~frm :message (.getMessage e#) :cause e#}
                                      ~ctx))))))))

(extend-protocol Dataflow
  clojure.lang.IFn
  (flow [f in-value]
    (protect-flow
     {:dataflow f :input-value in-value}
     (.invoke f in-value))))

(def ^:const missing ::missing)
(def ^:const option-missing ::option-missing)

(definline missing? [val]
  `(identical? ::missing ~val))

(definline option-missing? [val]
  `(identical? ::option-missing ~val))

;; ## Impl

(defn chain [& dflows]
  (dataflow
   [this value]
   (protect-flow
    {:validator this
     :validator-form (cons 'chain dflows)
     :input-value value}
    (reduce (fn [val dataflow]
              (if (instance? DataflowError val)
                (reduced val)
                (flow dataflow val)))
            value dflows))))

(defprotocol FieldValidator
  (map-extract [fv mapping key])
  (map-combine [fv tmap key value]))

(extend-protocol FieldValidator
  Object
  (map-extract [df mapping key]
    ;; map-extract-mark-missing
    (let [value (get mapping key missing)
          validated (protect-flow
                     {:field key :input-value value}
                     (flow df value))]
      (if (missing? validated)
        (dataflow-error {:dataflow df :message (format "`%s` is missing" key)
                         :field key :input-value missing})
        validated)))
  (map-combine [_ tmap key value]
    ;; map-combine-omit-missing
    (cond-> tmap (not (option-missing? value))
            (assoc! key value))))

(defn mapping [& {:as name-validators}]
  (dataflow
   [this value]
   (let [validated (persistent!
                    (reduce-kv (fn [result key validator]
                                 (map-combine validator result key
                                              (map-extract validator value key)))
                               (transient {})
                               name-validators))
         errors (filter #(instance? DataflowError (val %)) validated)]
     (if (seq errors)
       (dataflow-error {:dataflow this :message (format "Fields [%s] didn't validate"
                                                        (str/join ", " (map key errors)))
                        :cause (set (map val errors))
                        :validator-form (cons 'mapping (apply concat name-validators))
                        :input-value value
                        :partial-output (apply dissoc validated (map key errors))})
       validated))))

(defn optional
  "Transforms non-existing values into a default value.
   If there is a value, applies inner validator on it."
  ([inner & {:keys [default]
             :or {default option-missing}}]
   (dataflow
    [_ value]
    (if (missing? value)
      default (flow inner value)))))

(defn pattern
  "Checks against a regular expression, optionally returning a subgroup"
  ([regex] (pattern regex 0))
  ([regex group]
   (dataflow
    [this value]
    (if-let [match (re-matches regex value)]
      (cond
        (vector? match) (or (match group)
                            (dataflow-error {:dataflow this
                                             :message (format "No group #%d" group)
                                             :regex regex
                                             :input-value value}))
        (zero? group) match
        :else (dataflow-error {:dataflow this
                               :message "No subgroups"
                               :regex regex
                               :input-value value}))))))

(defmulti coercion
  (fn [value target-type]
    [(type value) target-type]))

(defmethod coercion :default
  [value ^Class target-type]
  (.cast target-type value))

(defmethod coercion [String Boolean] [^String value _] (Boolean/valueOf value))
(defmethod coercion [String Byte] [^String value _] (Byte/valueOf value))
(defmethod coercion [String Short] [^String value _] (Short/valueOf value))
(defmethod coercion [String Integer] [^String value _] (Integer/valueOf value))
(defmethod coercion [String Long] [^String value _] (Long/valueOf value))
(defmethod coercion [String Float] [^String value _] (Float/valueOf value))
(defmethod coercion [String Double] [^String value _] (Double/valueOf value))
(defmethod coercion [String BigInteger] [^String value _] (BigInteger. value))
(defmethod coercion [String BigDecimal] [^String value _] (BigDecimal. value))
(defmethod coercion [String clojure.lang.BigInt] [^String value _] (clojure.lang.BigInt/fromBigInteger (BigInteger. value)))

(defmethod coercion [Object String] [value _] (.toString ^Object value))
(defmethod coercion [Number Number] [value to] (coercion (str value) to))

(defn coerce
  "Coerce string to a specific type"
  [type]
  (dataflow
   [this value]
   (protect-flow {:dataflow this :coerce-to type :input-value value}
                   (coercion value type))))

(defn minmax
  "Check that integer is in range"
  ([max] (minmax 0 max))
  ([min max]
   (dataflow
    [this value]
    (if (and (>= value min) (<= value max))
      value
      (dataflow-error {:dataflow this :min min :max max :input-value value})))))

(defn pred [p?]
  (dataflow
   [this value]
   (if (p? value)
     value
     (dataflow-error {:dataflow this :pred p? :input-value value}))))

(defn opt-pred? [p?] (optional (pred p?)))

;; ## validators that work on maps

(defn tee [source-key target-key target-flow]
  (dataflow
   [this value]
   (let [v (map-extract target-flow value source-key)]
     (if (option-missing? v)
       value (assoc value target-key v)))))

(defn rename [& {:as renames}]
  (dataflow
   [this m]
   (loop [tm (apply dissoc! (transient m) (keys renames))
          [[from to :as entry] & rest] (seq renames)]
     (if (nil? entry) (persistent! tm)
         (recur (if (contains? m from)
                  (assoc! tm to (m from))
                  tm)
                rest)))))

(defn duplicate [& {:as dupes}]
  (dataflow
   [this m]
   (loop [tm (transient m)
          [[from to :as entry] & rest] (seq dupes)]
     (if (nil? entry) (persistent! tm)
         (recur (if (contains? m from)
                  (assoc! tm to (m from))
                  tm)
                rest)))))
