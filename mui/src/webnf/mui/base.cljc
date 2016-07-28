(ns webnf.mui.base
  #?@(:cljs
      [(:require-macros [webnf.mui.base :refer [defstyle]])
       (:require [webnf.base.util :include-macros true :as util]
                 [webnf.mui.style :refer [style-class]]
                 [clojure.string :as str])]))

#?
(:clj
 (do (defmacro defstyle [vname style]
       `(def ~vname (with-meta ~style
                      {:class-name ~(name vname)}))))
 :cljs
 (do (defn add-styles [props & styles]
       (let [cls (apply style-class styles)]
         (update props :class #(if (str/blank? %)
                                 cls (str % " " cls)))))))
