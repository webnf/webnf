(defproject webnf.deps/universe "0.1.19-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The uber dependency to get a full set of popular
  dependencies. For development or when you have room in your .m2 repo."
  :dependencies [[webnf.deps/logback "_"]
                 [webnf.deps/dev "_"]
                 [webnf.deps/web "_"]
                 [webnf.deps/contrib "_"]

                 ;; Algorithms
                 [com.lambdaworks/scrypt "1.4.0"]
                 [net.mikera/core.matrix "0.36.1"]
                 [instaparse "1.4.1"]
                 [nf.fr.eraasoft/objectpool "1.1.2"]
                 [clojurewerkz/meltdown "1.1.0"]
                 [net.polyc0l0r/hasch "0.3.0-beta2"]

                 ;; Data formats
                 [clj-time "0.10.0" :exclusions [joda-time]]
                 [net.mikera/vectorz-clj "0.30.1"]
                 [com.google.code.findbugs/jsr305  "_"] ;; reflections
                 [org.reflections/reflections "0.9.10"
                  :exclusions [com.google.code.findbugs/annotations]]
                 [danlentz/clj-uuid "0.1.6"]
                 [org.fressian/fressian "0.6.5"]
                 [cheshire "5.5.0"]

                 ;; Evaluation controllers
                 [com.stuartsierra/component "0.2.3"]
                 [de.kotka/lazymap "3.1.1"]

                 ;; APIs
                 [joda-time "2.8.1"]
                 [clj-http "1.1.2"
                  :exclusions [commons-logging]]
                 [amazonica "0.3.28" :exclusions [joda-time commons-logging]]])
