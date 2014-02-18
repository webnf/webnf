(ns webnf.middleware
  (:require
   [webnf.base :refer [autoload]]
   [clojure.tools.logging :as log]))

(autoload webnf.middleware.browser-http/wrap-browser-http)
(autoload webnf.middleware.pretty-exception/wrap-pretty-exception)
