(ns webnf.mui.theme
  (:require [garden.units :refer [px percent]]))

(declare ^:dynamic *current-theme*)
(defprotocol IThemeSelector
  (-select
    [sel theme]
    [sel theme not-found]))

(def resolve-selectors
  (memoize
   (fn [sheet-or-val theme]
     (cond (satisfies? IThemeSelector sheet-or-val)
           (recur (-select sheet-or-val theme) theme)
           ;; (map? sheet-or-val)
           (or (instance? PersistentArrayMap sheet-or-val)
               (instance? PersistentHashMap sheet-or-val))
           (persistent!
            (reduce-kv (fn [tr k v]
                         (let [v' (resolve-selectors v theme)]
                           (if (identical? v v')
                             tr (assoc! tr k v'))))
                       (transient sheet-or-val) sheet-or-val))
           :else
           sheet-or-val))))

(defrecord SheetSelector [path]
  IThemeSelector
  (-select [_ theme]
    (resolve-selectors
     (get-in theme path)
     theme))
  (-select [_ theme not-found]
    (resolve-selectors
     (get-in theme path not-found)
     theme))
  IDeref
  (-deref [this]
    (-select this *current-theme*)))
(defrecord ValueSelector [sheet-selector key]
  IThemeSelector
  (-select [_ theme]
    (resolve-selectors
     (get (-select sheet-selector theme) key)
     theme))
  (-select [_ theme not-found]
    (resolve-selectors
     (get (-select sheet-selector theme) key not-found)
     theme))
  IDeref
  (-deref [this]
    (-select this *current-theme*)))

(def theme-sheet
  (memoize
   (fn [& path]
     (->SheetSelector path))))
(def theme-value
  (memoize
   (fn [& path]
     (->ValueSelector (apply theme-sheet (butlast path))
                      (last path)))))

;; Base Theme

(defn get-color
  ([color] (get-color color :p50))
  ([color shade] (theme-value :colors color shade)))

(def base-theme
  (let [sans-serif "'Roboto', 'Helvetica Neue', Helvetica, Arial"]
    {:overlay
     {:base {:height "100%",
             :position "fixed",
             :width "100%",
             :top 0,
             :left 0,
             :bottom 0,
             :right 0,
             :background-color "rgb(0,0,0)",
             :opacity ".01",
             :z-index "2",
             :visibility "hidden",
             :transition "visibility 0s linear .4s, opacity .4s cubic-bezier(.4, 0, .2, 1), z-index 0s linear .4s"}
      :visible {:opacity ".3",
                :filter "alpha(opacity=30)",
                :visibility "visible",
                :transition "visibility 0s linear 0s, opacity .4s cubic-bezier(.4, 0, .2, 1), z-index 0s linear 0s"}}
     :shadows
     {:base {:bottom 0 :left 0 :right 0 :top 0
             :position "absolute"
             :will-change "box-shadow"
             :transition "box-shadow 0.28s cubic-bezier(0.4,0,0.2,1)"}
      :z1top {:box-shadow "0 2px 10px 0 rgba(0, 0, 0, 0.16)"}
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
     {:app-bar {:height (px 56)}
      :button  {:height (px 18)
                :min-width (px 88)}}
     :typography
     {:display4 {:color "rgba(0, 0, 0, .54)",
                 :font-family sans-serif,
                 :font-weight 300,
                 :font-size (px 112)
                 :white-space "nowrap"}
      :display3 {:color "rgba(0, 0, 0, .54)",
                 :font-family sans-serif,
                 :font-weight 400,
                 :font-size (px 56)
                 :white-space "nowrap"}
      :display2 {:color "rgba(0, 0, 0, .54)",
                 :font-family sans-serif,
                 :font-weight 400,
                 :font-size (px 45)}
      :display1 {:color "rgba(0, 0, 0, .54)",
                 :font-family sans-serif,
                 :font-weight 400,
                 :font-size (px 34)}
      :headline {:color "rgba(0, 0, 0, .87)",
                 :font-family sans-serif,
                 :font-weight 400,
                 :font-size (px 24)}
      :title {:color "rgba(0, 0, 0, .87)",
              :font-family sans-serif,
              :font-weight 500,
              :font-size (px 20)
              :white-space "nowrap"}
      :subhead {:color "rgba(0, 0, 0, .87)",
                :font-family sans-serif,
                :font-weight 400,
                :font-size (px 16)}
      :body2 {:color "rgba(0, 0, 0, .87)",
              :font-family sans-serif,
              :font-weight 500,
              :font-size (px 14)
              :line-height (px 0)}
      :body1 {:color "rgba(0, 0, 0, .87)",
              :font-family sans-serif,
              :font-weight 400,
              :font-size (px 14)}
      :caption {:color "rgba(0, 0, 0, .54)",
                :font-family sans-serif,
                :font-weight 400,
                :font-size (px 12),
                :white-space "nowrap"}
      :menu {:color "rgba(0, 0, 0, .87)",
             :font-family sans-serif,
             :font-weight 500,
             :font-size (px 14)
             :white-space "nowrap"}
      :button {:color "rgba(0, 0, 0, .87)",
               :font-family sans-serif,
               :font-weight 500,
               :font-size (px 14)
               :white-space "nowrap",
               :text-transform "uppercase"}}
     :text-field
     (let [text-margin "0.5em 0 0.25em 0"
           focus-color (get-color :blue :p500)
           label-color (get-color :grey :p500)]
       {:normal {:background :transparent
                 :font-family sans-serif
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
               :height (theme-value :sizes :button :height)
               :min-width (theme-value :sizes :button :min-width)
               :border-radius (px 3)
               :display :inline-block
               :outline :none
               :padding (px 9)
               :text-align :center
               :user-select :none
               :line-height (px 16)
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
               :height (theme-value :sizes :app-bar :height)
               :top 0
               :width (percent 100)
               :z-index 1
               :margin 0
               :padding 0}
      :title {:display :inline-block
              :opacity :inherit
              :cursor :default
              :line-height (theme-value :sizes :app-bar :height)
              :position :absolute
              :top 0
              :left (px 16)}
      :children {:display :inline-block
                 :position :absolute
                 :right (px 16)
                 :top (px 10)}}
     :colors
     {:red 
      {:p50 "#fde0dc"
       :p100 "#f9bdbb"
       :p200 "#f69988"
       :p300 "#f36c60"
       :p400 "#e84e40"
       :p500 "#e51c23"
       :p600 "#dd191d"
       :p700 "#d01716"
       :p800 "#c41411"
       :p900 "#b0120a"
       :a100 "#ff7997"
       :a200 "#ff5177"
       :a400 "#ff2d6f"
       :a700 "#e00032"}
      :pink 
      {:p50 "#fce4ec"
       :p100 "#f8bbd0"
       :p200 "#f48fb1"
       :p300 "#f06292"
       :p400 "#ec407a"
       :p500 "#e91e63"
       :p600 "#d81b60"
       :p700 "#c2185b"
       :p800 "#ad1457"
       :p900 "#880e4f"
       :a100 "#ff80ab"
       :a200 "#ff4081"
       :a400 "#f50057"
       :a700 "#c51162"}
      :purple 
      {:p50 "#f3e5f5"
       :p100 "#e1bee7"
       :p200 "#ce93d8"
       :p300 "#ba68c8"
       :p400 "#ab47bc"
       :p500 "#9c27b0"
       :p600 "#8e24aa"
       :p700 "#7b1fa2"
       :p800 "#6a1b9a"
       :p900 "#4a148c"
       :a100 "#ea80fc"
       :a200 "#e040fb"
       :a400 "#d500f9"
       :a700 "#aa00ff"}
      :deep-purple 
      {:p50 "#ede7f6"
       :p100 "#d1c4e9"
       :p200 "#b39ddb"
       :p300 "#9575cd"
       :p400 "#7e57c2"
       :p500 "#673ab7"
       :p600 "#5e35b1"
       :p700 "#512da8"
       :p800 "#4527a0"
       :p900 "#311b92"
       :a100 "#b388ff"
       :a200 "#7c4dff"
       :a400 "#651fff"
       :a700 "#6200ea"}
      :indigo 
      {:p50 "#e8eaf6"
       :p100 "#c5cae9"
       :p200 "#9fa8da"
       :p300 "#7986cb"
       :p400 "#5c6bc0"
       :p500 "#3f51b5"
       :p600 "#3949ab"
       :p700 "#303f9f"
       :p800 "#283593"
       :p900 "#1a237e"
       :a100 "#8c9eff"
       :a200 "#536dfe"
       :a400 "#3d5afe"
       :a700 "#304ffe"}
      :blue 
      {:p50 "#e7e9fd"
       :p100 "#d0d9ff"
       :p200 "#afbfff"
       :p300 "#91a7ff"
       :p400 "#738ffe"
       :p500 "#5677fc"
       :p600 "#4e6cef"
       :p700 "#455ede"
       :p800 "#3b50ce"
       :p900 "#2a36b1"
       :a100 "#a6baff"
       :a200 "#6889ff"
       :a400 "#4d73ff"
       :a700 "#4d69ff"}
      :light-blue 
      {:p50 "#e1f5fe"
       :p100 "#b3e5fc"
       :p200 "#81d4fa"
       :p300 "#4fc3f7"
       :p400 "#29b6f6"
       :p500 "#03a9f4"
       :p600 "#039be5"
       :p700 "#0288d1"
       :p800 "#0277bd"
       :p900 "#01579b"
       :a100 "#80d8ff"
       :a200 "#40c4ff"
       :a400 "#00b0ff"
       :a700 "#0091ea"}
      :cyan 
      {:p50 "#e0f7fa"
       :p100 "#b2ebf2"
       :p200 "#80deea"
       :p300 "#4dd0e1"
       :p400 "#26c6da"
       :p500 "#00bcd4"
       :p600 "#00acc1"
       :p700 "#0097a7"
       :p800 "#00838f"
       :p900 "#006064"
       :a100 "#84ffff"
       :a200 "#18ffff"
       :a400 "#00e5ff"
       :a700 "#00b8d4"}
      :teal 
      {:p50 "#e0f2f1"
       :p100 "#b2dfdb"
       :p200 "#80cbc4"
       :p300 "#4db6ac"
       :p400 "#26a69a"
       :p500 "#009688"
       :p600 "#00897b"
       :p700 "#00796b"
       :p800 "#00695c"
       :p900 "#004d40"
       :a100 "#a7ffeb"
       :a200 "#64ffda"
       :a400 "#1de9b6"
       :a700 "#00bfa5"}
      :green 
      {:p50 "#d0f8ce"
       :p100 "#a3e9a4"
       :p200 "#72d572"
       :p300 "#42bd41"
       :p400 "#2baf2b"
       :p500 "#259b24"
       :p600 "#0a8f08"
       :p700 "#0a7e07"
       :p800 "#056f00"
       :p900 "#0d5302"
       :a100 "#a2f78d"
       :a200 "#5af158"
       :a400 "#14e715"
       :a700 "#12c700"}
      :light-green 
      {:p50 "#f1f8e9"
       :p100 "#dcedc8"
       :p200 "#c5e1a5"
       :p300 "#aed581"
       :p400 "#9ccc65"
       :p500 "#8bc34a"
       :p600 "#7cb342"
       :p700 "#689f38"
       :p800 "#558b2f"
       :p900 "#33691e"
       :a100 "#ccff90"
       :a200 "#b2ff59"
       :a400 "#76ff03"
       :a700 "#64dd17"}
      :lime 
      {:p50 "#f9fbe7"
       :p100 "#f0f4c3"
       :p200 "#e6ee9c"
       :p300 "#dce775"
       :p400 "#d4e157"
       :p500 "#cddc39"
       :p600 "#c0ca33"
       :p700 "#afb42b"
       :p800 "#9e9d24"
       :p900 "#827717"
       :a100 "#f4ff81"
       :a200 "#eeff41"
       :a400 "#c6ff00"
       :a700 "#aeea00"}
      :yellow 
      {:p50 "#fffde7"
       :p100 "#fff9c4"
       :p200 "#fff59d"
       :p300 "#fff176"
       :p400 "#ffee58"
       :p500 "#ffeb3b"
       :p600 "#fdd835"
       :p700 "#fbc02d"
       :p800 "#f9a825"
       :p900 "#f57f17"
       :a100 "#ffff8d"
       :a200 "#ffff00"
       :a400 "#ffea00"
       :a700 "#ffd600"}
      :amber 
      {:p50 "#fff8e1"
       :p100 "#ffecb3"
       :p200 "#ffe082"
       :p300 "#ffd54f"
       :p400 "#ffca28"
       :p500 "#ffc107"
       :p600 "#ffb300"
       :p700 "#ffa000"
       :p800 "#ff8f00"
       :p900 "#ff6f00"
       :a100 "#ffe57f"
       :a200 "#ffd740"
       :a400 "#ffc400"
       :a700 "#ffab00"}
      :orange 
      {:p50 "#fff3e0"
       :p100 "#ffe0b2"
       :p200 "#ffcc80"
       :p300 "#ffb74d"
       :p400 "#ffa726"
       :p500 "#ff9800"
       :p600 "#fb8c00"
       :p700 "#f57c00"
       :p800 "#ef6c00"
       :p900 "#e65100"
       :a100 "#ffd180"
       :a200 "#ffab40"
       :a400 "#ff9100"
       :a700 "#ff6d00"}
      :deep-orange 
      {:p50 "#fbe9e7"
       :p100 "#ffccbc"
       :p200 "#ffab91"
       :p300 "#ff8a65"
       :p400 "#ff7043"
       :p500 "#ff5722"
       :p600 "#f4511e"
       :p700 "#e64a19"
       :p800 "#d84315"
       :p900 "#bf360c"
       :a100 "#ff9e80"
       :a200 "#ff6e40"
       :a400 "#ff3d00"
       :a700 "#dd2c00"}
      :brown 
      {:p50 "#efebe9"
       :p100 "#d7ccc8"
       :p200 "#bcaaa4"
       :p300 "#a1887f"
       :p400 "#8d6e63"
       :p500 "#795548"
       :p600 "#6d4c41"
       :p700 "#5d4037"
       :p800 "#4e342e"
       :p900 "#3e2723"}
      :grey 
      {:p50 "#fafafa"
       :p100 "#f5f5f5"
       :p200 "#eeeeee"
       :p300 "#e0e0e0"
       :p400 "#bdbdbd"
       :p500 "#9e9e9e"
       :p600 "#757575"
       :p700 "#616161"
       :p800 "#424242"
       :p900 "#212121"
       :p1000 "#000000"
       :p1000_ "#ffffff"}
      :blue-grey 
      {:p50 "#eceff1"
       :p100 "#cfd8dc"
       :p200 "#b0bec5"
       :p300 "#90a4ae"
       :p400 "#78909c"
       :p500 "#607d8b"
       :p600 "#546e7a"
       :p700 "#455a64"
       :p800 "#37474f"
       :p900 "#263238"}}}))

(def ^:dynamic *current-theme* base-theme)
