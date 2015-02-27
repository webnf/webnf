(defproject webnf.deps/universe "0.1.0-alpha8"
  :plugins [[lein-modules "0.3.10"]]
  :description "The uber dependency to get a full set of popular
  dependencies. For development or when you have room in your .m2 repo."
  :dependencies [[webnf.deps/logback "_"]
                 [webnf.deps/dev "_"]
                 [webnf.deps/web "_"]
                 [webnf.deps/contrib "_"]

                 ;; Algorithms
                 [com.lambdaworks/scrypt "1.4.0"]
                 [net.mikera/core.matrix "0.33.2"]
                 [instaparse "1.3.5"]
                 [nf.fr.eraasoft/objectpool "1.1.2"]
                 [clojurewerkz/meltdown "1.1.0"]
                 [net.polyc0l0r/hasch "0.3.0-beta2"]

                 ;; Data formats
                 [clj-time "0.9.0" :exclusions [joda-time]]
                 [net.mikera/vectorz-clj "0.29.0"]
                 [org.reflections/reflections "0.9.9"]
                 [danlentz/clj-uuid "0.1.2-20150217.010720-1"]
                 [org.fressian/fressian "0.6.5"]
                 [cheshire "5.4.0"]

                 ;; Evaluation controllers
                 [com.stuartsierra/component "0.2.2"]
                 [de.kotka/lazymap "3.1.1"]

                 ;; APIs
                 [joda-time "2.7"]
                 [clj-http "1.0.1"]
                 [amazonica "0.3.19" :exclusions [joda-time]]
                 [javax.mail/javax.mail-api "1.5.2"]])
