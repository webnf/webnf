(ns webnf.js.dom)

(defn dom-clone!
  "Clone dom nodes. This draws on the usual convention to represent
  markup as a node or collection of nodes"
  [node-or-nodes]
  (if (sequential? node-or-nodes)
    (map #(.cloneNode % true) node-or-nodes)
    (.cloneNode node-or-nodes true)))
