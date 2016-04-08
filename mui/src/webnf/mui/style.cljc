(ns webnf.mui.style
  #?@(:cljs
      [(:require-macros webnf.mui.style)
       (:require [garden.core :refer [css]]
                 [goog.style :as sty]
                 [clojure.string :as str])]))

#?
(:cljs
 (do
   ;; Style rendering

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


   ;; Style caching

   (def ^:private style-counter (volatile! 0))
   (def ^:private style-cache (volatile! {}))

   (defn init! [style-counter* style-cache*]
     (if (zero? @style-counter)
       (do (vreset! style-counter style-counter*)
           (vreset! style-cache style-cache*))
       (throw (ex-info "Cannot reset style cache, already used" {:style-counter @style-counter
                                                                 :style-cache @style-cache}))))

   (defn gen-class-name []
     (str "c-" (vswap! style-counter inc)))

   (defn intern-style
     "Gets the class name for a particular style. Memoizing."
     ([style] (intern-style style true))
     ([style install]
      (or (get @style-cache style)
          (let [cls (gen-class-name)]
            (when install
              (doto (sty/installStyles
                     (render-style cls style))
                (.setAttribute "id" (str cls "-styles"))))
            (vswap! style-cache assoc style cls)
            cls))))

   (defn style-class [& styles]
     (str/join " " (map intern-style (flatten-styles styles))))))
