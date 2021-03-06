(defproject webnf.deps/universe "0.1.19-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The uber dependency to get a full set of popular
  dependencies. For development or when you have room in your .m2 repo."
  :dependencies [[webnf.deps/logback "_" :upgrade false]
                 [webnf.deps/dev "_" :upgrade false]
                 [webnf.deps/web "_" :upgrade false]
                 [webnf.deps/contrib "_" :upgrade false]

                 ;; Algorithms
                 [com.lambdaworks/scrypt "1.4.0"]
                 [net.mikera/core.matrix "0.50.1"]
                 [instaparse "1.4.1"]
                 [nf.fr.eraasoft/objectpool "1.1.2"]
                 [clojurewerkz/meltdown "1.1.0"]
                 [net.polyc0l0r/hasch "0.3.0-beta2" :exclusions [org.clojure/clojurescript com.google.guava/guava]]

                 ;; Data formats
                 [clj-time "0.11.0" :exclusions [joda-time]]
                 [net.mikera/vectorz-clj "0.43.1"]
                 [com.google.code.findbugs/jsr305  "_" :upgrade false] ;; reflections
                 [org.reflections/reflections "0.9.10"
                  :exclusions [com.google.code.findbugs/annotations com.google.guava/guava]]
                 [danlentz/clj-uuid "0.1.6"]
                 [org.fressian/fressian "0.6.6"]
                 [cheshire "5.5.0"]

                 ;; Evaluation controllers
                 [com.stuartsierra/component "_" :upgrade false]
                 [de.kotka/lazymap "3.1.1"]

                 ;; APIs
                 [joda-time "_" :upgrade false]
                 [clj-http "2.1.0" :exclusions [commons-logging]]
                 [amazonica "0.3.52" :exclusions [joda-time commons-logging]]])
