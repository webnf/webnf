(ns webnf.middleware
  (:require [clojure.tools.logging :as log]))

(defmacro ^:private autoload [var-name]
  (let [vns (namespace var-name)
        vn (name var-name)]
    `(defn ~vn [~'& args#]
       (log/trace ~(str "Autoloading " vns))
       (require '~vns)
       (let [var# (var ~var-name)]
         (def ~vn var#)
         (apply var# args#)))))

(autoload webnf.middleware.browser-http/wrap-browser-http)
(autoload webnf.middleware.pretty-exception/wrap-pretty-exception)
