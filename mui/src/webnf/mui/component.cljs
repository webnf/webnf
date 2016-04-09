(ns webnf.mui.component
  (:require-macros [webnf.mui.base :refer [defcomponent component]])
  (:require
   [webnf.base.logging :as log]
   [webnf.mui.base :refer [add-styles]]
   [webnf.mui.style :refer [style-class]]
   [webnf.mui.theme :refer [theme-sheet]]
   [om.next :as om]
   [om-tools.dom :as dom]
   [clojure.string :as str]
   [garden.units :refer [px s percent]]
   [garden.compiler :refer [render-css]]))

(defcomponent overlay [:show]
  [this {:keys [show] :as props}]
  (dom/div (-> props
               (add-styles @(theme-sheet :overlay :base)
                           (when show @(theme-sheet :overlay :visible))))
           (om/children this)))

(defcomponent shadow [:size]
  [this {:keys [size] :as props
         :or {size 1}}]
  (dom/div
   (dissoc props :size)
   (dom/div {:class (style-class @(theme-sheet :shadows :base)
                                 @(theme-sheet :shadows (keyword (str "z" size "bottom"))))})
   (dom/div {:class (style-class @(theme-sheet :shadows :base)
                                 @(theme-sheet :shadows (keyword (str "z" size "top"))))}
            (om/children this))))

(defcomponent app-bar [:title]
  [this {:keys [title] :as props}]
  (dom/div
   (dissoc props :title)
   (dom/nav
    {:class (style-class @(theme-sheet :app-bar :normal))}
    (when title
      (dom/div {:class (style-class
                        @(theme-sheet :typography :title)
                        @(theme-sheet :app-bar :title))}
               title))
    (when-let [ch (seq (om/children this))]
      (apply dom/div {:class (style-class @(theme-sheet :app-bar :children))}
             ch)))))

(defcomponent app []
  [this {:keys [top left bottom right content dimensions] :as props}]
  (dom/div
   (-> props
       (dissoc :top :left :bottom :right :content :dimensions)
       (add-styles {:position :absolute
                    :top 0 :left 0 :bottom 0 :right 0}))
   (when top
     (dom/div {:style #js{:height (render-css (:top dimensions))}
               :class (style-class {:position :absolute :top 0 :width (percent 100)
                                    :overflow :hidden})}
              top))
   (when left
     (dom/div {:style #js{:top (render-css (:top dimensions))
                          :bottom (render-css (:bottom dimensions))
                          :width (render-css (:left dimensions))}
               :class (style-class {:position :absolute :left 0 :overflow :hidden})}
              left))
   (when content
     (dom/div {:style #js{:left (render-css (:left dimensions))
                          :right (render-css (:right dimensions))
                          :top (render-css (:top dimensions))
                          :bottom (render-css (:bottom dimensions))}
               :class (style-class {:position :absolute :overflow :hidden})}
              content))
   (when right
     (dom/div {:style #js{:top (render-css (:top dimensions))
                          :bottom (render-css (:bottom dimensions))
                          :width (render-css (:right dimensions))}
               :class (style-class {:position :absolute :right 0 :overflow :hidden})}
              right))
   (when bottom
     (dom/div {:style #js{:height (render-css (:bottom dimensions))}
               :class (style-class {:position :absolute :top 0 :width (percent 100)
                                    :overflow :hidden})}
              bottom))
   (om/children this)))
