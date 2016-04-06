(defproject webnf/base "0.2.0-SNAPSHOT"
  :description "Collection org.clojure libs and essential others"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.clojure/clojure "_" :upgrade false]
                 [webnf.deps/logback "_" :upgrade false]
                 ;; a few essential contribs
                 [org.clojure/core.async "_" :exclusions [org.clojure/tools.analyzer.jvm] :upgrade false]
                 [org.clojure/tools.analyzer.jvm "_" :upgrade false]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/core.typed "_" :classifier "slim" :upgrade false]
                 [org.clojure/core.unify "0.5.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.nrepl "_" :upgrade false]])
