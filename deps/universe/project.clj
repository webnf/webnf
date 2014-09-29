(defproject webnf.deps/universe "0.0.1-SNAPSHOT"
  :description "The uber dependency to get a full set of popular
  dependencies. For development or when you have room in your .m2 repo."
  :url "http://github.com/webnf/webnf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[webnf.deps/core "0.0.5-SNAPSHOT"]
                 [webnf.deps/dev "0.0.3-SNAPSHOT"]
                 [webnf.deps/web "0.0.4-SNAPSHOT"]
                 [webnf.deps/contrib "0.0.4-SNAPSHOT"]

                 ;; Algorithms
                 [com.lambdaworks/scrypt "1.4.0"]
                 [net.mikera/core.matrix "0.29.1"]
                 [instaparse "1.3.3"]

                 ;; Data formats
                 [clj-time "0.8.0"]
                 [net.mikera/vectorz-clj "0.25.0"]
                 [org.reflections/reflections "0.9.9-RC2"]

                 ;; Evaluation controllers
                 [com.stuartsierra/component "0.2.2"]
                 [de.kotka/lazymap "3.1.1"]

                 ;; APIs
                 [clj-http "1.0.0"]
                 [amazonica "0.2.25"]])
