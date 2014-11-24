(defproject webnf.deps/universe "0.1.0-alpha3"
  :plugins [[lein-modules "0.3.9"]]
  :description "The uber dependency to get a full set of popular
  dependencies. For development or when you have room in your .m2 repo."
  :dependencies [[webnf.deps/logback "_"]
                 [webnf.deps/dev "_"]
                 [webnf.deps/web "_"]
                 [webnf.deps/contrib "_"]

                 ;; Algorithms
                 [com.lambdaworks/scrypt "1.4.0"]
                 [net.mikera/core.matrix "0.31.1"]
                 [instaparse "1.3.4"]

                 ;; Data formats
                 [clj-time "0.9.0-beta1"]
                 [net.mikera/vectorz-clj "0.26.2"]
                 [org.reflections/reflections "0.9.9"]

                 ;; Evaluation controllers
                 [com.stuartsierra/component "0.2.2"]
                 [de.kotka/lazymap "3.1.1"]

                 ;; APIs
                 [clj-http "1.0.1"]
                 [amazonica "0.2.30" :exclusions [joda-time]]])
