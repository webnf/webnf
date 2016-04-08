(ns webnf.js.zip
  (:require webnf.js.jszip))

(defn zip []
  (js/JSZip.))

(defn add-file! [zip name content]
  (.file zip name content))

(defn generate! [zip & {:keys [type]
                        :or {type "blob"}}]
  (.generate zip #{:type type}))
