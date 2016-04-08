(ns webnf.js.save-as
  (:require webnf.js.file-saver))

(defn save-as [name file]
  (js/saveAs file name))

