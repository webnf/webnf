(ns webnf.mui.styles)

(defmacro defstyle [vname style]
  `(def ~vname (with-meta ~style
                 {:class-name ~(name vname)})))

