(ns webnf.cljs.enlive.transform
  (:require
   [goog.dom :as dom]
   [webnf.cljs.enlive :as sel]
   [clojure.string :as str]))

(defn- transform-node! [node previous-state transformations]
  (when-let [state (sel/step previous-state node)]
    (doseq [child (sel/child-nodes node)]
         (transform-node! child state transformations))
    (when-let [k (sel/accept-key state)]
      ((transformations k) node))))

(defn lockstep-transform! [nodes transformations-map]
  (let [state (sel/lockstep-automaton (keys transformations-map))
        transformations (vec (map #(or % (constantly nil))
                                  (vals transformations-map)))]
    (doseq [n nodes]
      (transform-node! n state transformations))))

(defn at* [node-or-nodes rules]
  (lockstep-transform! (sel/as-nodes node-or-nodes) rules))

(defn instantiate [node & {:as rules}]
  (apply at* (.cloneNode node true) rules))

;; transformations

(defn to-nodes [nodes]
  (let [frag (.createDocumentFragment js/document)]
    (doseq [n (remove (some-fn sequential? nil?) (tree-seq sequential? seq nodes))]
      (dom/appendChild
       frag (cond
              (string? n) (.createTextNode js/document n)
              (number? n) (.createTextNode js/document (str n))
              :else       n)))
    frag))

(defn set-attr [& {:as attr-vals}]
  #(doseq [[a v] attr-vals]
     (.setAttribute % (str a) (str v))))

(defn add-class [& classes]
  #(.setAttribute % "class"
                  (str/join " "
                            (into (set classes)
                                  (sel/attr-values % "class")))))

(defn content [& children]
  #(do
     (dom/removeChildren %)
     (dom/appendChild % (to-nodes children))))

(defn append [& children]
  #(dom/appendChild % (to-nodes children)))

(defn substitute [& children]
  #(do
     (dom/replaceNode % (to-nodes children))))

(defn do-> [& transforms]
  (fn [node]
    (doseq [t transforms]
      (t node))))
