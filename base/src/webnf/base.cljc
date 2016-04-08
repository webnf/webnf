(ns webnf.base
  "Various primitive core operations that should be 'just there'"
  (:require [webnf.base.autoload
             #?@(:cljs [:refer-macros [autoload autoload-some]])]
            #?@(:cljs [webnf.base.util cljs.pprint cljs.repl])))


#?
(:clj
 (do
   (webnf.base.autoload/autoload ^:macro ^:static webnf.base.autoload/autoload)
   (autoload ^:macro ^:static webnf.base.autoload/autoload-some)
   (autoload ^:static clojure.pprint/pprint)
   (autoload-some ^:static (clojure.repl pst source doc))
   (autoload-some ^:static (webnf.base.platform hostname local-ip reset-logging-config! pr-cls))
   (autoload-some ^:static (webnf.base.util
                            ^:macro forcat ^:macro static-case
                            ^:macro defunrolled ^:macro squelch))))
(autoload-some ^:static (webnf.base.util to-many pprint-str
                                         pretial ap rcomp scat))
