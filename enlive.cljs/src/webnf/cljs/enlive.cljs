(ns webnf.cljs.enlive
  (:require
   [goog.dom :as dom]
   [clojure.string :as str]
   [webnf.util :refer [xhr dom-clone!]]
   [webnf.promise :refer [promise]]
   [cljs.core.async :refer [<! map<]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(defn to-str [i]
  (cond
    (keyword? i) (name i)
    (symbol? i) (name i)
    :default (str i)))

(defn child-nodes [node]
  ;; reify live views like NodeList and HTMLCollection
  (into [] (array-seq (dom/getChildren node))))

(def any (constantly true))

(defn tag=
  "Selector predicate, :foo is as short-hand for (tag= :foo)."
  [tag-name]
  (let [tag-name (.toUpperCase (to-str tag-name))]
    #(= (.-tagName %) tag-name)))

(defn attr= [attr val]
  (let [as (to-str attr)]
    #(= (.getAttribute % as) val)))

(defn id=
  "Selector predicate, :#foo is as short-hand for (id= \"foo\")."
  [id]
  (attr= :id id))

(defn attr-values
  "Returns the whitespace-separated values of the specified attr as a set or nil."
  [node attr]
  (when-let [v (.getAttribute node (to-str attr))]
    (set (str/split v #"\s+"))))

(defn attr-has
  "Selector predicate, tests if the specified whitespace-seperated attribute contains the specified values. See CSS ~="
  [attr & values]
  #(when-let [v (attr-values % attr)]
     (every? v values)))

(defn has-class
  "Selector predicate, :.foo.bar is as short-hand for (has-class \"foo\" \"bar\")."
  [& classes]
  (apply attr-has :class classes))

;; selector syntax
(defn intersection [preds]
  (condp = (count preds)
    1 (first preds)
    2 (let [[f g] preds] #(and (f %) (g %)))
    3 (let [[f g h] preds] #(and (f %) (g %) (h %)))
    4 (let [[f g h k] preds] #(and (f %) (g %) (h %) (k %)))
    (fn [x] (every? #(% x) preds))))

(defn union [preds]
  (condp = (count preds)
    1 (first preds)
    2 (let [[f g] preds] #(or (f %) (g %)))
    3 (let [[f g h] preds] #(or (f %) (g %) (h %)))
    4 (let [[f g h k] preds] #(or (f %) (g %) (h %) (k %)))
    (fn [x] (some #(% x) preds))))

(defn- filter-prefix [fc segments]
  (->> segments
       (filter #(= fc (.charAt % 0)))
       (map #(subs % 1))))

(def ^:private compile-keyword
  (memoize
   (fn [kw]
     (if (= :> kw)
       :>
       (let [[tag-name :as segments] (str/split (name kw) #"(?=[#.])")
             classes (filter-prefix \. segments)
             ids (filter-prefix \# segments)
             preds (when-not (contains? #{nil \* \# \.}
                                        (.charAt tag-name 0))
                     (list (tag= tag-name)))
             preds (if (seq classes)
                     (conj preds (apply has-class classes))
                     preds)
             preds (case (count ids)
                     0 preds
                     1 (conj preds (id= (first ids)))
                     (throw (js/Error. (str "More than one id specified in selector " kw))))]
         (if (seq preds) (intersection preds) any))))))

(defn compile-step [step]
  (cond
    (keyword? step) (compile-keyword step)
    (set? step) (union (map compile-step step))
    (vector? step) (intersection (map compile-step step))
    :else step))

(defn select-step [node step]
  ((compile-step step) node))

;; selector chains

(defn- compile-chain [chain]
  (map compile-step chain))

(defn- selector-chains [selector id]
  (for [x (tree-seq set? seq selector) :when (not (set? x))]
    (compile-chain (concat x [id]))))

(defn- predset [preds]
  (condp = (count preds)
    1 (let [[f] preds] #(if (f %) 1 0))
    2 (let [[f g] preds] #(+ (if (f %) 1 0) (if (g %) 2 0)))
    3 (let [[f g h] preds] #(-> (if (f %) 1 0) (+ (if (g %) 2 0))
                              (+ (if (h %) 4 0))))
    4 (let [[f g h k] preds] #(-> (if (f %) 1 0) (+ (if (g %) 2 0))
                                (+ (if (h %) 4 0)) (+ (if (k %) 8 0))))
    #(loop [i 1 r 0 preds (seq preds)]
       (if-let [[pred & preds] preds]
         (recur (bit-shift-left i 1) (if (pred %) (+ i r) r) preds)
         r))))

(defn- states [init chains-seq]
  (fn [^Number n]
    (loop [n n s (set init) [chains & etc] chains-seq]
      (cond
        (odd? n) (recur (bit-shift-right n 1) (into s chains) etc)
        (zero? n) s
        :else (recur (bit-shift-right n 1) s etc)))))

(defn- make-state [chains]
  (let [derivations
          (reduce
            (fn [derivations chain]
              (cond
                (= :> (first chain))
                  (let [pred (second chain)]
                    (assoc derivations pred (conj (derivations pred) (nnext chain))))
                (next chain)
                  (let [pred (first chain)]
                    (-> derivations
                      (assoc nil (conj (derivations nil) chain))
                      (assoc pred (conj (derivations pred) (next chain)))))
                :else
                  (assoc derivations :accepts (first chain)))) {} chains)
        always (derivations nil)
        accepts (derivations :accepts)
        derivations (dissoc derivations nil :accepts)
        ps (predset (keys derivations))
        next-states (memoize #(make-state ((states always (vals derivations)) %)))]
    [accepts (when (seq chains) (comp next-states ps))]))

(defn cacheable [selector] (vary-meta selector assoc ::cacheable true))
(defn cacheable? [selector] (-> selector meta ::cacheable))

(defn- automaton* [selector]
  (make-state (-> selector (selector-chains 0) set)))

(defn- lockstep-automaton* [selectors]
  (make-state (set (mapcat selector-chains selectors (iterate inc 0)))))

(def ^{:private true} memoized-automaton* (memoize automaton*))

(def ^{:private true} memoized-lockstep-automaton* (memoize lockstep-automaton*))

(defn automaton [selector]
  ((if (cacheable? selector) memoized-automaton* automaton*) selector))

(defn lockstep-automaton [selectors]
  ((if (every? cacheable? selectors) memoized-lockstep-automaton* lockstep-automaton*) selectors))

(defn accept-key [s] (nth s 0))
(defn step [s x] (when-let [f (and s (nth s 1))] (f x)))

(defn fragment-selector? [selector]
  (map? selector))

(defn node-selector? [selector]
  (not (fragment-selector? selector)))

(defn- static-selector? [selector]
  (or (keyword? selector)
      (and (coll? selector) (every? static-selector? selector))))

(defn select-nodes* [nodes state]
  (letfn [(select1 [node previous-state]
            (when-let [state (step previous-state node)]
              (let [descendants (mapcat #(select1 % state) (child-nodes node))]
                (if (accept-key state) (cons node descendants) descendants))))]
    (mapcat #(select1 % state) nodes)))

(defn select-nodes [nodes selector]
  (select-nodes* nodes (automaton selector)))

(defn select-fragments* [nodes state-from state-to]
  (letfn [(select1 [nodes previous-state-from previous-state-to]
            (when (and previous-state-from previous-state-to)
              (let [states-from (map #(step previous-state-from %) nodes)
                    states-to (map #(step previous-state-to %) nodes)
                    descendants (reduce into []
                                  (map #(select1 (child-nodes %1) %2 %3)
                                    nodes states-from states-to))]
                (loop [fragments descendants fragment nil
                       nodes nodes states-from states-from states-to states-to]
                  (if-let [[node & etc] (seq nodes)]
                    (if fragment
                      (let [fragment (conj fragment node)]
                        (if (accept-key (first states-to))
                          (recur (conj fragments fragment) nil etc
                            (rest states-from) (rest states-to))
                          (recur fragments fragment etc
                            (rest states-from) (rest states-to))))
                      (if (accept-key (first states-from))
                        (recur fragments [] nodes states-from states-to)
                        (recur fragments nil etc
                          (rest states-from) (rest states-to))))
                    fragments)))))]
    (select1 nodes state-from state-to)))

(defn select-fragments [nodes selector]
  (let [[selector-from selector-to] (first selector)
        state-from (automaton selector-from)
        state-to (automaton selector-to)]
    (select-fragments* nodes state-from state-to)))

(defn as-nodes [node-or-nodes]
  (cond
    (seqable? node-or-nodes) node-or-nodes
    (= js/DocumentFragment (type node-or-nodes)) (child-nodes node-or-nodes)
    :else (cons node-or-nodes nil)))

(defn select
  "Returns the seq of nodes or fragments matched by the specified selector."
  [node-or-nodes selector]
  (let [nodes (as-nodes node-or-nodes)]
    (if (node-selector? selector)
      (select-nodes nodes selector)
      (select-fragments nodes selector))))

(defn select-1 [node-or-nodes selector]
  (let [result (select node-or-nodes selector)]
    (when (next result)
      (throw (js/Error. (array "Multiple select result: " node-or-nodes selector))))
    (first result)))

(defn load-html* [uri]
  (go (let [{:keys [status header body] :as resp}
            (<! (xhr uri))]
        (if (= status 200)
          body
          (throw (ex-info "Request error" {:uri uri :response resp}))))))

(defn load-html [uri & {selector :selector}]
  (map< dom-clone!
        (promise ;; todo replace this with comet update
         (go (let [body (<! (load-html* uri))
                   snip (dom/htmlToDocumentFragment body)]
               (if selector
                 (select snip selector)
                 snip))))))

;; other selection helpers

(defn parent
  ([child this-one?]
     (loop [child child]
       (if (or (nil? child) (this-one? child)) child
           (recur (.-parentElement child))))))

;;

(def root (cacheable (compile-chain [:> :*])))
