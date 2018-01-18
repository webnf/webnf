(ns webnf.base
  "Various primitive core operations that should be 'just there'"
  (:refer-clojure :exclude [update-in])
  (:require [webnf.base.autoload
             #?@(:cljs [:refer-macros [autoload autoload-some]])]
            #?@(:cljs [webnf.base.util cljs.pprint cljs.repl])))

;; The following form should be quite interesting. First note, that
;; although it is read by host clojure only, it provides many macros,
;; that can be utilized by clojurescript, as well.
#?
(:clj ;; The only remaining ...
 (do ;; ... stuff are jvm ...
   (webnf.base.autoload/autoload ^:macro ^:static webnf.base.autoload/autoload)
   (autoload ^:macro ^:static webnf.base.autoload/autoload-some)
   (autoload ^:static clojure.pprint/pprint)
   (autoload-some ^:static (clojure.repl pst source doc))
   ;; ... platform specific functions.
   (autoload-some ^:static (webnf.base.platform hostname local-ip reset-logging-config! pr-cls))
   (autoload-some ^:static (webnf.base.util
                            ^:macro forcat ^:macro static-case
                            ^:macro defunrolled ^:macro squelch
                            ^:macro deprecated-alias))))

;; Here are some cross-platform functions

(autoload-some ^:static (webnf.base.util to-coll pprint-str
                                         pretial ap rcomp scat
                                         path->href href->path
                                         update-in str-quote
                                         string-builder append!
                                         conjv conjs conjm conjq))

;; Deprecated aliases. A recent addition, to the webnf zoo. In the
;; spirit of clojure, we won't rename functions, without leaving an
;; intact program for everybody. We will, however, be shouting
;; warnings through the logging system!

                                        ; Also, there is no effort put
                                        ; into optimizing deprecated
                                        ; call sites

(deprecated-alias to-many to-coll)
