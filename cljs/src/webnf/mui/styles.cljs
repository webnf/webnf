(ns webnf.mui.styles
  (:require [garden.units :refer [px percent]]
            [garden.types :refer [map->CSSUnit]]
            [garden.color :refer [map->CSSColor]]
            [clojure.string :as str]
            [garden.core :refer [css]]
            [goog.style :as sty]
            [webnf.mui.colors :refer [get-color]]
            [webnf.util :refer-macros [forcat log-expr]]
            [cljs.reader :as r]))

(defn- nest-sel [])

(defn- arr-cat [c1 c2]
  (.. js/Array -prototype
      (concat (into-array c1) (into-array c2))))

(defn conj-classes [c1 c2]
  (str/join " " (if (str/blank? c1)
                  c2
                  (cons c1 c2))))

(defn- substyle? [[k _]] (vector? k))
(defn- render-style [cls style]
  (let [style* (into {} (remove substyle? style))
        substyles (filter substyle? style)]
    (str
     "/** Style: " (pr-str (meta style)) " */\n"
     (css (vec
           (list* (str "." cls) style*
                  (for [[sel sty] substyles]
                    (reduce #(vector %2 %1) sty (reverse sel)))))))))

(def ^:private flatten-styles
  (memoize
   (fn
     ([styles] (persistent! (flatten-styles (transient []) styles)))
     ([so-far-t styles]
      (if (map? styles)
        (conj! so-far-t styles)
        (reduce flatten-styles so-far-t styles))))))

(def style-counter (volatile! 0))
(def style-cache (volatile! {}))

(defn setup-precompiled! [counter cache]
  (vreset! style-counter counter)
  (vreset! style-cache cache))

(defn gen-class-name []
  (str "c" (vswap! style-counter inc)))

(defn intern-style [style]
  (or (get @style-cache style)
      (let [cls (gen-class-name)
            se (sty/installStyles
                (render-style cls style))]
        (vswap! style-cache assoc style cls)
        (.setAttribute se "id" (str cls "-styles"))
        cls)))

(defn intern-styles [styles]
  (doall (map intern-style styles)))

(defn render-styles [style-cache]
  (apply str (for [[style cls] style-cache]
               (render-style cls style))))

(defn style-classes [styles]
  (intern-styles (flatten-styles styles)))

(defn style-class [styles]
  (str/join " " (style-classes styles)))

(defn with-style [f args & content]
  (if-let [s (:styles args)]
    (apply f (-> args
                 (dissoc :styles)
                 (update :class conj-classes (style-classes s)))
           content)
    (apply f #js{:class (style-classes args)} content)))

(defn with-default-style [f default-style args & content]
  (apply with-style f (update args :styles #(if (map? %1) [%2 %1] %2)
                              default-style)
         content))

(def font-family "'Roboto', 'Helvetica Neue', Helvetica, Arial")
(def all-styles
  {:shadows
   {:z1top {:box-shadow "0 2px 10px 0 rgba(0, 0, 0, 0.16)"}
    :z1bottom {:box-shadow "0 2px 5px 0 rgba(0, 0, 0, 0.26)"}
    :z2top {:box-shadow "0 6px 20px 0 rgba(0, 0, 0, 0.19)"}
    :z2bottom {:box-shadow "0 8px 17px 0 rgba(0, 0, 0, 0.2)"}
    :z3top {:box-shadow "0 17px 50px 0 rgba(0, 0, 0, 0.19)"}
    :z3bottom {:box-shadow "0 12px 15px 0 rgba(0, 0, 0, 0.24)"}
    :z4top {:box-shadow "0 25px 55px 0 rgba(0, 0, 0, 0.21)"}
    :z4bottom {:box-shadow "0 16px 28px 0 rgba(0, 0, 0, 0.22)"}
    :z5top {:box-shadow "0 40px 77px 0 rgba(0, 0, 0, 0.22)"}
    :z5bottom {:box-shadow "0 27px 24px 0 rgba(0, 0, 0, 0.2)"}}
   :sizes
   {:app-bar-sizing {:height (px 56)}
    :button-sizing  {:height (px 36)
                     :min-width (px 88)}}
   :typography
   {:display4 {:color "rgba(0, 0, 0, .54)",
               :font-family font-family,
               :font-weight 300,
               :font-size (px 112)
               :white-space "nowrap"}
    :display3 {:color "rgba(0, 0, 0, .54)",
               :font-family font-family,
               :font-weight 400,
               :font-size (px 56)
               :white-space "nowrap"}
    :display2 {:color "rgba(0, 0, 0, .54)",
               :font-family font-family,
               :font-weight 400,
               :font-size (px 45)}
    :display1 {:color "rgba(0, 0, 0, .54)",
               :font-family font-family,
               :font-weight 400,
               :font-size (px 34)}
    :headline {:color "rgba(0, 0, 0, .87)",
               :font-family font-family,
               :font-weight 400,
               :font-size (px 24)}
    :title {:color "rgba(0, 0, 0, .87)",
            :font-family font-family,
            :font-weight 500,
            :font-size (px 20)
            :white-space "nowrap"}
    :subhead {:color "rgba(0, 0, 0, .87)",
              :font-family font-family,
              :font-weight 400,
              :font-size (px 16)}
    :body2 {:color "rgba(0, 0, 0, .87)",
            :font-family font-family,
            :font-weight 500,
            :font-size (px 14)
            :line-height (px 0)}
    :body1 {:color "rgba(0, 0, 0, .87)",
            :font-family font-family,
            :font-weight 400,
            :font-size (px 14)}
    :caption {:color "rgba(0, 0, 0, .54)",
              :font-family font-family,
              :font-weight 400,
              :font-size (px 12),
              :white-space "nowrap"}
    :menu {:color "rgba(0, 0, 0, .87)",
           :font-family font-family,
           :font-weight 500,
           :font-size (px 14)
           :white-space "nowrap"}
    :button {:color "rgba(0, 0, 0, .87)",
             :font-family font-family,
             :font-weight 500,
             :font-size (px 14)
             :white-space "nowrap",
             :text-transform "uppercase"}}
   :text-field
   (let [text-margin "0.5em 0 0.25em 0"
         focus-color (get-color :blue :p500)
         label-color (get-color :grey :p500)]
     {:normal {:background :transparent
               :font-family font-family
               :font-size (px 16)
               :border :none
               :outline :none
               :left 0
               :width (percent 100)
               :padding 0
               :margin text-margin}
      :underline {:background-color label-color :height (px 1)}
      :focussed-underline {:background-color focus-color
                           :height (px 2)
                           :position :absolute
                           :top 0 :left 0 :right 0 :opacity 0
                           :transition "left 0.2s ease-out, right 0.2s ease-out"}
      :error-underline {:background-color (get-color :red :p400)}
      :container {:position "relative"
                  :width (px 300)
                  :padding-bottom (px 8)
                  :display :inline-block}
      :underline-container {:position :relative
                            :left 0 :right 0 :height 0
                            :overflow :visible}
      :full-width {:width (percent 100)}
      :place-holder {:color label-color
                     :font-size (px 16)
                     :left (px 1)
                     :position :absolute
                     :opacity 1
                     :transition "top .18s linear, font-size .18s linear, opacity .10s linear"
                     :pointer-events :none
                     :margin text-margin}
      :floating-label {:top (px 27)}
      :floating-label-top {:font-size (px 12) :top (px 4)}
      :scroll-blocks {:background-color label-color
                      :bottom (px 6)
                      :height (px 3)
                      :opacity 0
                      :position :absolute
                      :transition "opacity .28s linear"
                      :width 3}
      :focus-label {:color focus-color}
      :focus {:background-color focus-color}})
   :button
   {:normal {:webkit-tap-highlight-color "rgba(0,0,0,0)"
             :cursor :pointer
             :position :relative
             :height (px 18)
             :min-width (px 88)
             :border-radius (px 3)
             :display :inline-block
             :outline :none
             :padding (px 9)
             :text-align :center
             :user-select :none
             :line-height (px 0)
             :border :none
             :background-color "rgba(255,255,255,0)"}
    :raised {:background-color (get-color :grey :p300)}
    :disabled {:background-color (get-color :grey :p300)
               :color (get-color :grey :p500)
               :cursor :default}}
   :app-bar
   {:normal {:background-color (get-color :cyan :a700)
             :box-sizing :border-box
             :position :relative
             :height (px 56)
             :top 0
             :width (percent 100)
             :z-index 1}
    :title {:display :inline-block
            :opacity :inherit
            :cursor :default
            :line-height (px 56)
            :position :absolute
            :top 0
            :left (px 16)}
    :children {:display :inline-block
               :position :absolute
               :right (px 16)
               :top 0}}})

(defn get-style [cat sty]
  (with-meta (get-in all-styles [cat sty])
    {:class-name (str (name cat) "-" (name sty))}))
