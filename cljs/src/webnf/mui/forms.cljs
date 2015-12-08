(ns webnf.mui.forms
  (:require
   [om.core :as om]
   [om-tools.dom :as dom]
   [goog.dom :as gdom]
   [clojure.string :as str])
  (:require-macros
   [webnf.mui.base :refer [defwrapped]]))

(defn add-class
  ([base cls] (cond
                (nil? base)  #{cls}
                (coll? base) (conj base cls)
                :else        (str base " " cls)))
  ([base cls & classes] (reduce #(add-class %1 %2)
                                (add-class base cls) classes)))

(defwrapped group dom/div [f attrs content]
  (apply f (update attrs :class add-class "mui-form-group") content))

(defwrapped label dom/label [f attrs content]
  (apply f (update attrs :class add-class "mui-form-floating-label") content))

(defwrapped input dom/input [f attrs content]
  (apply f (update attrs :class add-class "mui-form-control") content))

