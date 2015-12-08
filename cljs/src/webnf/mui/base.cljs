(ns webnf.mui.base
  (:require
   [om.core :as om]))

(defn state-setter [owner key]
  (fn [e]
    (om/set-state! owner key (.. e -target -value))))
