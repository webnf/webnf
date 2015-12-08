(ns webnf.mui.components
  (:require [clojure.string :as str]
            [garden.units :refer [px s percent]]
            [goog.dom :as gdom]
            [goog.events :as evt]
            [goog.events.EventType :as ET]
            [goog.style :as gsty]
            [webnf.mui.colors :refer [get-color]]
            [webnf.mui.styles
             :refer [style-class get-style conj-classes]
             :refer-macros [defstyle]]
            [om.core :as om :include-macros true]
            [om-tools.dom :as dom]
            [webnf.util :refer [log-pr log]])
  (:require-macros [webnf.mui.base :refer [defelement component defcomponent defcomponent-kv]])
  (:import goog.dom.ViewportSizeMonitor))

(defstyle overlay-style
  {:height "100%",
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
   :transition "visibility 0s linear .4s, opacity .4s cubic-bezier(.4, 0, .2, 1), z-index 0s linear .4s"})

(defstyle overlay-visible-style
  {:opacity ".3",
   :filter "alpha(opacity=30)",
   :visibility "visible",
   :transition "visibility 0s linear 0s, opacity .4s cubic-bezier(.4, 0, .2, 1), z-index 0s linear 0s"})

(defstyle shadow-style
  {:bottom 0 :left 0 :right 0 :top 0
   :position "absolute"
   :will-change "box-shadow"
   :transition "box-shadow 0.28s cubic-bezier(0.4,0,0.2,1)"})

(defstyle dialog-normal-style
  {:background-color (get-color :grey),
   :box-sizing "border-box",
   :line-height (px 24),
   :padding (px 24)
   :left "50%",
   :top "50%",
   :position "fixed",
   :transform "translate(-50%,-50%) scale(1, 1)",
   :transform-origin "0 0",
   :z-index "3",
   :will-change "transform, opacity, left, top"})
(defstyle dialog-title-style
  {:padding-bottom (px 10)})
(defstyle dialog-children-style
  {:position "relative"})

(comment def transition-end-name
         (when js/document
           (let [div (.createElement js/document "div")]
             (reduce-kv (fn [_ k v]
                          (when (aget (.-style div) k)
                            (reduced v)))
                        nil
                        {"WebkitTransition" "webkitTransitionEnd"
                         "MozTransition" "transitionend"
                         "OTransition" "oTransitionEnd otransitionend"
                         "transition" "transitionend"}))))

(defelement overlay [{:keys [show on-click]} children]
  (dom/div {:class (style-class
                    (cond-> [overlay-style]
                      show (conj overlay-visible-style)))
            :onClick on-click}
           children))


(defelement shadow [{:keys [size style]} children]
  (let [[ts bs] (case size
                  1 [:z1top :z1bottom]
                  2 [:z2top :z2bottom]
                  3 [:z3top :z3bottom]
                  4 [:z4top :z4bottom]
                  5 [:z5top :z5bottom]
                  [nil nil])]
    (dom/div
     (dom/div {:class (style-class [shadow-style (and bs (get-style :shadows bs)) style])})
     (dom/div {:class (style-class [shadow-style (and ts (get-style :shadows ts)) style])}
              children))))


(defelement dialog [{:keys [title width show]} children]
  (dom/div {:class (style-class dialog-normal-style)
            :style #js{:visibility (if show "visible" "hidden")}}
           (shadow {:size 3})
           (when title
             (dom/div {:class (style-class
                               [(get-style :typography :title)
                                dialog-title-style])}
                      title))
           (dom/div {:class (style-class dialog-children-style)}
                    children)))

(def mk-style (comp style-class get-style))

(defcomponent-kv text-field [{:keys [type value place-holder default-value floating-label error name
                                     style on-change on-blur on-focus on-enter grab-focus]}
                             owner opts]

  (reify
    om/IDisplayName (display-name [_] "text-field")
    om/IInitState (init-state [_] {:focus false :focussing false :current-value value})
    om/IDidMount
    (did-mount [_]
      (let [text-field (om/get-node owner "text-field")
            ul-cont (om/get-node owner "underline-container")]
        (om/set-state! owner :mounted true)
        (om/set-state! owner :text-field text-field)
        (om/set-state! owner :ul-cont ul-cont)
        (when grab-focus
          (.focus text-field))))
    om/IRenderState
    (render-state
      [_ {:keys [focus focussing focus-x current-value mounted]}]
      (let [focus-x (volatile! nil)
            {:keys [scroll-left scroll-width width ulr]}
            (when mounted
              (let [text-field (om/get-state owner :text-field)
                    underline-container (om/get-state owner :ul-cont)]
                {:scroll-left (.-scrollLeft text-field)
                 :scroll-width (.-scrollWidth text-field)
                 :width (.-offsetWidth text-field)
                 :ulr (.getBoundingClientRect underline-container)}))
            do-on-change (fn [e]
                           (om/set-state! owner :current-value (.. e -target -value))
                           (when on-change (on-change e)))
            do-on-focus (fn [e]
                          (om/set-state! owner :focus true)
                          (when-let [x @focus-x]
                            (vreset! focus-x nil)
                            (om/set-state! owner :focussing x)
                            (js/setTimeout #(om/set-state! owner :focussing false) 1))
                          (when on-focus (on-focus e)))
            do-on-blur (fn [e]
                         (om/set-state! owner :focus false)
                         (when on-blur (on-blur e)))]
        (dom/div {:class (style-class
                          [(get-style :text-field :container)
                           (when floating-label
                             {:height (px 66)})
                           style])}
                 (when place-holder
                   (dom/div {:class (style-class
                                     (list*
                                      (get-style :text-field :place-holder)
                                      (when floating-label
                                        (get-style :text-field :floating-label))
                                      (when (or focus (not (str/blank? current-value)))
                                        (if floating-label
                                          (cons (get-style :text-field :floating-label-top)
                                                (when focus
                                                  [(get-style :text-field :focus-color)]))
                                          [{:opacity 0}]))))}
                            place-holder))
                 (dom/input {:class (style-class [(get-style :text-field :normal)
                                                  (when floating-label
                                                    {:padding-top (px 25)})])
                             :value current-value
                             :ref "text-field"
                             :name name
                             :type type
                             :defaultValue default-value
                             :onChange do-on-change
                             :onKeyDown (when on-enter
                                          (fn [e]
                                            (when (= 13 (.-keyCode e))
                                              (on-enter e))))
                             :onKeyUp do-on-change
                             :onClick do-on-change
                             :onWheel do-on-change
                             :onFocus do-on-focus
                             :onBlur do-on-blur
                             :onMouseDown (fn [e]
                                            (when-not focus
                                              (vreset! focus-x (.-clientX e))))
                             :onTouchStart (fn [e]
                                             (when-not focus
                                               (vreset! focus-x (.. e -touches (item 0) -clientX))))})
                 (dom/div {:class (mk-style :text-field :underline-container)
                           :ref "underline-container"}
                          (dom/div {:class (mk-style :text-field :underline)
                                    :ref "underline"})
                          (dom/div {:class (style-class
                                            [(get-style :text-field :focussed-underline)
                                             (when focus
                                               (if (and ulr focussing)
                                                 {:opacity 1
                                                  :transition :none
                                                  :left (px (- focussing (.-left ulr)))
                                                  :right (px (- (.-right ulr) focussing))}
                                                 {:opacity 1}))
                                             (when error (get-style :text-field :error-underline))])
                                    :ref "focussed-underline"}))
                 (dom/div {:class (style-class
                                   [(get-style :text-field :scroll-blocks)
                                    (when scroll-left {:opacity 1})
                                    (when (om/get-state owner :focus) (get-style :text-field :focus))
                                    {:left (px 6)}])})
                 (dom/div {:class (style-class
                                   [(get-style :text-field :scroll-blocks)
                                    (when (> scroll-width (+ scroll-left width))
                                      {:opacity 1})
                                    (when (om/get-state owner :focus) (get-style :text-field :focus))
                                    {:right (px 6)}])}))))))

(defcomponent button [{:keys [type style raised disabled on-click]} label owner _]
  (reify
    om/IDisplayName (display-name [_] "button")
    om/IInitState (init-state [_] {:active (not disabled)})
    om/IRenderState
    (render-state [_ {:keys [active]}]
      (let [props {:tabIndex "0"
                   :class (style-class
                           [(get-style :typography :button)
                            (get-style :button :normal)
                            (when raised (get-style :button :raised))
                            (when disabled (get-style :button :disabled))
                            {:display "inline-block" :position "relative"}
                            style])}]
        (dom/div (assoc props
                        :onClick (when-not disabled on-click)
                        :onMouseDown #(om/set-state! owner :active false)
                        :onMouseUp #(om/set-state! owner :active true)
                        :onMouseLeave #(om/set-state! owner :active true)
                        :role "button")
                 (shadow {:style {:border-radius (px 3)}
                          :size (cond (not raised) -1
                                      active 2
                                      :else 1)})
                 (case type
                   :submit (dom/input (assoc props :type "submit" :value (apply str label)))
                   :button (dom/button props label)
                   label))))))

(defelement modal-dialog [{:as props :keys [actions on-submit on-dismiss on-submit-label on-dismiss-label show]}
                          children]
  (dom/div
   (overlay {:show show :on-click on-dismiss})
   (dialog props
           (dom/form
            {:onSubmit (fn [e]
                                        ;(.stopPropagation e)
                         (.preventDefault e)
                         (when on-submit (on-submit e)))
             :autoComplete "on"
             :method "POST"}
            children
            (dom/div {:style #js{:textAlign "right"
                                 :marginTop "20px"
                                 :marginBottom "-10px"}}
                     actions
                     (when on-dismiss
                       (button {:style {:color (get-color :red :p500)}
                                :on-click on-dismiss}
                               (or on-dismiss-label "Dismiss")))
                     (when on-submit
                       (button {:type :submit
                                :style {:color (get-color :blue :p500)}}
                               (or on-submit-label "Submit"))))))))

(defelement app-bar [{:keys [title]} children]
  (dom/div
   (dom/nav
    {:class (style-class
             [(get-style :app-bar :normal)])}
    (when title
      (dom/div {:class (style-class
                        [(get-style :typography :title)
                         (get-style :app-bar :title)])}
               title))
    (when (seq children)
      (apply dom/div {:class (style-class [(get-style :app-bar :children)])}
             children)))))

(defelement app [{:keys [top left bottom right content dimensions]} children]
  (dom/div
   {:class (style-class {:position :absolute
                         :top 0 :left 0 :bottom 0 :right 0})}
   (when top
     (dom/div {:style #js{:height (:top dimensions)}
               :class (style-class {:position :absolute :top 0 :width (percent 100)
                                    :overflow :hidden})}
              top))
   (when left
     (dom/div {:style #js{:top (:top dimensions)
                          :bottom (:bottom dimensions)
                          :width (:left dimensions)}
               :class (style-class {:position :absolute :left 0 :overflow :hidden})}
              left))
   (when content
     (dom/div {:style #js{:left (:left dimensions)
                          :right (:right dimensions)
                          :top (:top dimensions)
                          :bottom (:bottom dimensions)}
               :class (style-class {:position :absolute :overflow :hidden})}
              content))
   (when right
     (dom/div {:style #js{:top (:top dimensions)
                          :bottom (:bottom dimensions)
                          :width (:right dimensions)}
               :class (style-class {:position :absolute :right 0 :overflow :hidden})}
              right))
   (when bottom
     (dom/div {:style #js{:height (:bottom dimensions)}
               :class (style-class {:position :absolute :top 0 :width (percent 100)
                                    :overflow :hidden})}
              bottom))
   children))

(defonce vsm
  (when js/window
    (ViewportSizeMonitor. js/window)))

(defn measuring-container [cmp]
  (fn [cur owner opts]
    (reify
      om/IDisplayName
      (display-name [_]
        "measuring-container")
      om/IDidMount
      (did-mount [_]
        (let [measure! (fn [] (om/refresh! owner))]
          (om/set-state! owner :listener-id (evt/listen vsm ET/RESIZE measure!))
          (om/set-state! owner :node (om/get-node owner))))
      om/IWillUnmount
      (will-unmount [_]
        (evt/unlistenByKey (om/get-state owner :listener-id)))
      om/IRenderState
      (render-state [_ {:keys [listener-id win-size]}]
        (dom/div
         {:class (style-class {:width (percent 100) :height (percent 100)})}
         (if listener-id
           (let [s (gsty/getSize (om/get-state owner :node))]
             (om/build cmp cur {:opts (assoc opts
                                             :width (.-width s)
                                             :height (.-height s))}))
           (dom/div)))))))

(defelement paper [{:keys [size style]} children]
  (dom/div
   {:class (style-class
            [{:position :relative :display :inline-block
              :padding {:left (px 15) :right (px 15)
                        :top (px 10) :bottom (px 10)}}
             style])}
   (when (and size (pos? size))
     (shadow {:size size}))
   (dom/div
    {:class (style-class {:position :relative})}
    children)))

(defelement scroll-box [{:keys [style x y] :as has-cfg
                           :or {x :auto y :auto}}
                          children]
  (dom/div
   {:class (style-class
            [{:position :relative
              :width (percent 100)
              :height (percent 100)}
             (when x {:overflow-x x})
             (when y {:overflow-y y})
             style])}
   children))
