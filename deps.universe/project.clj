(defproject webnf.deps/universe "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.9"]]
  :description "The uber dependency to get a full set of popular
  dependencies. For development or when you have room in your .m2 repo."
  :dependencies [[webnf.deps/logback "_"]
                 [webnf.deps/dev "_"]
                 [webnf.deps/web "_"]
                 [webnf.deps/contrib "_"]

                 ;; Algorithms
                 [com.lambdaworks/scrypt "1.4.0"]
                 [net.mikera/core.matrix "0.30.2"]
                 [instaparse "1.3.4"]

                 ;; Data formats
                 [clj-time "0.8.0"]
                 [net.mikera/vectorz-clj "0.26.1"]
                 [org.reflections/reflections "0.9.9"]

                 ;; Evaluation controllers
                 [com.stuartsierra/component "0.2.2"]
                 [de.kotka/lazymap "3.1.1"]

                 ;; APIs
                 [clj-http "1.0.0"]
                 [amazonica "0.2.28" :exclusions [joda-time]]])
