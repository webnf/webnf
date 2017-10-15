(defproject webnf.deps/universe "0.2.0-alpha3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The uber dependency to get a full set of popular
  dependencies. For development or when you have room in your .m2 repo."
  :dependencies [[webnf.deps/logback "_" :upgrade false]
                 [webnf.deps/dev "_" :upgrade false]
                 [webnf.deps/web "_" :upgrade false]
                 [webnf.deps/contrib "_" :upgrade false]

                 ;; Algorithms
                 [com.lambdaworks/scrypt "1.4.0"]
                 [net.mikera/core.matrix "0.61.0"]
                 [instaparse "1.4.8"]
                 [nf.fr.eraasoft/objectpool "1.1.2"]
                 [clojurewerkz/meltdown "1.1.0"]
                 [net.polyc0l0r/hasch "0.3.0-beta2" :exclusions [org.clojure/clojurescript com.google.guava/guava]]

                 ;; Data formats
                 [clj-time "0.14.0" :exclusions [joda-time]]
                 [net.mikera/vectorz-clj "0.47.0"]
                 [com.google.code.findbugs/jsr305  "3.0.2"] ;; reflections
                 [org.reflections/reflections "0.9.11"
                  :exclusions [com.google.code.findbugs/annotations com.google.guava/guava]]
                 [danlentz/clj-uuid "0.1.7"]
                 [org.fressian/fressian "0.6.6"]
                 [cheshire "5.8.0"]

                 ;; Evaluation controllers
                 [com.stuartsierra/component "0.3.2"]
                 [de.kotka/lazymap "3.1.1"]

                 ;; APIs
                 [joda-time "2.9.9"]
                 [clj-http "3.7.0" :exclusions [commons-logging]]
                 [amazonica "0.3.113" :exclusions [joda-time commons-logging]]

                 ;; Services
                 [http-kit "2.3.0-alpha4"]])
