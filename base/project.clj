(defproject webnf/base "0.2.0-alpha3-SNAPSHOT"
  :description "Collection org.clojure libs and essential others"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.clojure/clojure "1.9.0-beta1"]
                 [webnf.deps/logback "0.2.0-alpha2"]
                 ;; a few essential contribs
                 [org.clojure/test.check "0.10.0-alpha2"]
                 [org.clojure/core.async "0.3.443" :exclusions [org.clojure/tools.analyzer.jvm]]
                 [org.clojure/tools.analyzer.jvm "0.7.1"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [org.clojure/core.typed "0.4.1" :classifier "slim"]
                 [org.clojure/core.unify "0.5.7"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/tools.nrepl "0.2.13"]]
  :profiles
  {:dev {:dependencies [[org.clojure/clojurescript "1.9.946"]
                        [webnf.deps/dev "_" :upgrade false]]}})
