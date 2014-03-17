(ns webnf.async
  (:require [clojure.core.async :refer [<!!]]))

(defn chan-seq
  "A lazy seq that gets its elements by taking from chan."
  [ch]
  (lazy-seq
   (when-let [x (<!! ch)]
     (cons x (chan-seq ch)))))
