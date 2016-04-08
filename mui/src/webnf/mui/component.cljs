(ns webnf.mui.component
  (:require-macros [webnf.mui.base :refer [defcomponent component]])
  (:require
   webnf.mui.base
   [webnf.mui.style :refer [style-class]]
   [webnf.mui.theme :refer [theme-sheet]]
   [om-tools.dom :as dom]
   [clojure.string :as str]))

(defcomponent overlay [:show :children]
  [this {:keys [show children] :as props}]
  (let [cls (style-class @(theme-sheet :overlay :base)
                         (when show @(theme-sheet :overlay :visible)))]
    (dom/div (-> props
                 (update :class #(if (str/blank? %)
                                   cls (str % " " cls)))
                 (dissoc :children))
             children)))

(defcomponent shadow [:size]
    [this {:keys [size] :as props}]
    (dom/div
     (dom/div {:class (style-class @(theme-sheet :shadows :base)
                                   @(theme-sheet :shadows (keyword (str "z" size "bottom"))))})
     (dom/div {:class (style-class @(theme-sheet :shadows :base)
                                   @(theme-sheet :shadows (keyword (str "z" size "top"))))}
              children)))


