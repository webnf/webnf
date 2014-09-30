(ns webnf.base
  "Various primitive core operations that should be 'just there'"
  (:require webnf.base.autoload))

(webnf.base.autoload/autoload ^:macro webnf.base.autoload/autoload)
(autoload ^:macro ^:static webnf.base.autoload/autoload-some)
(autoload ^:static clojure.pprint/pprint)
(autoload-some ^:static (clojure.repl pst source doc))
(autoload-some ^:static (webnf.base.platform hostname reset-logging-config! pr-cls))
(autoload-some ^:static (webnf.base.utils to-many squelch pprint-str ^:macro forcat) )
