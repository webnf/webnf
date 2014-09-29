(ns webnf.base
  "Various primitive core operations that should be 'just there'"
  (:require webnf.base.autoload))

(webnf.base.autoload/autoload ^:macro webnf.base.autoload/autoload)
(autoload ^:macro webnf.base.autoload/autoload-some)
(autoload clojure.pprint/pprint)
(autoload-some (clojure.repl pst source doc))
(autoload-some (webnf.base.platform hostname reset-logging-config!))
(autoload-some (webnf.base.utils to-many squelch pprint-str ^:macro forcat) )


