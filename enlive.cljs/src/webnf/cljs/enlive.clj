(ns webnf.cljs.enlive
  (:require
   [clojure.string :as str]))

;; selector syntax
(defn intersection [preds]
  (condp = (count preds)
    1 (first preds)
    2 (let [[f g] preds] `#(and (~f %) (~g %)))
    3 (let [[f g h] preds] `#(and (~f %) (~g %) (~h %)))
    4 (let [[f g h k] preds] `#(and (~f %) (~g %) (~h %) (~k %)))
    `(fn [x#] (every? #(% x#) ~preds))))

(defn union [preds]
  (condp = (count preds)
    1 (first preds)
    2 (let [[f g] preds] `#(or (~f %) (~g %)))
    3 (let [[f g h] preds] `#(or (~f %) (~g %) (~h %)))
    4 (let [[f g h k] preds] `#(or (~f %) (~g %) (~h %) (~k %)))
    `(fn [x#] (some #(% x#) ~preds))))


(defn filter-prefix [fc segments]
  (->> segments
       (filter #(= fc (.charAt % 0)))
       (map #(subs % 1))))

(defn compile-keyword [kw]
  (if (= :> kw)
    :>
    (let [[tag-name :as segments] (str/split (name kw) #"(?=[#.])")
          classes (filter-prefix \. segments)
          ids (filter-prefix \# segments)
          preds (when-not (contains? #{nil \* \# \.}
                                     (.charAt tag-name 0))
                  (list `(lib.select/tag= ~tag-name)))
          preds (if (seq classes)
                  (conj preds `(lib.select/has-class ~@classes))
                  preds)
          preds (case (count ids)
                  0 preds
                  1 (conj preds `(lib.select/id= ~(first ids)))
                  (throw (ex-info "More than one id specified" {:selector kw})))]
      (if (seq preds) (intersection preds) `lib.select/any))))

(defn compile-step* [step]
  (cond
    (keyword? step) (compile-keyword step)
    (set? step) (union (map compile-step* step))
    (vector? step) (intersection (map compile-step* step))
    :else step))

(defmacro compile-step [step]
  (compile-step* step))

(defmacro select-step [node step]
  (list (compile-step* step) node))

(defn cacheable [selector] (vary-meta selector assoc ::cacheable true))

(defn static-selector? [selector]
  (or (keyword? selector)
      (and (coll? selector) (every? static-selector? selector))))

(defmacro let-select-1 [parent bindings & body]
  {:pre [(zero? (mod (count bindings) 2))]}
  (let [parent-sym (gensym "parent-")]
    `(let ~(vec (list*
                 parent-sym parent
                 (mapcat (fn [[bind selector]]
                           [bind `(select-1 ~parent-sym ~(if (static-selector? selector)
                                                           (cacheable selector)
                                                           selector))])
                         (partition 2 bindings))))
       ~@body)))

(defmacro let-select [parent bindings & body]
  {:pre [(zero? (mod (count bindings) 2))]}
  (let [parent-sym (gensym "parent-")]
    `(let ~(vec (list*
                 parent-sym parent
                 (mapcat (fn [[bind selector]]
                           [bind `(select ~parent-sym ~(if (static-selector? selector)
                                                         (cacheable selector)
                                                         selector))])
                         (partition 2 bindings))))
       ~@body)))
