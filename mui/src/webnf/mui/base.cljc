(ns webnf.mui.base
  #?@(:cljs
      [(:require-macros [webnf.mui.base :refer [component defcomponent defstyle]]
                        [om.next :refer [ui defui]])
       (:require [om.next :as om]
                 [om-tools.dom :as dom]
                 [webnf.base.util :include-macros true :as util]
                 [webnf.mui.style :refer [style-class]]
                 [clojure.string :as str])]))

#?
(:clj
 (do (defmacro component [display-name query [this props & add-methods] & body]
       (let [cls (with-meta (gensym "mui_") {:anonymous true})]
         `(do
            ~(list* 'om.next/defui cls
                    'static 'om.next/IQuery (list 'query '[_] query)
                    'Object (list 'render [this]
                                  (list* 'let [props (list 'om.next/props this)]
                                         body))
                    add-methods)
            (set! (.. ~cls ~'-prototype ~'-constructor ~'-displayName) ~display-name)
            (let [factory# (om.next/factory ~cls)]
              (vary-meta
               (fn [& [a# & r# :as attrs+contents#]]
                 (if (map? a#)
                   (apply factory# a# r#)
                   (apply factory# {} attrs+contents#)))
               #(merge (meta factory#)
                       %
                       {::class ~cls}))))))
     (defmacro defcomponent [name & component-body]
       `(def ~name (component ~(str name) ~@component-body)))
     (defmacro defstyle [vname style]
       `(def ~vname (with-meta ~style
                      {:class-name ~(name vname)}))))
 :cljs
 (do (defn get-query [factory]
       (if-let [cls (::class (meta factory))]
         (om/get-query cls)
         (om/get-query factory)))
     (defn add-styles [props & styles]
       (let [cls (apply style-class styles)]
         (update props :class #(if (str/blank? %)
                                 cls (str % " " cls)))))))
