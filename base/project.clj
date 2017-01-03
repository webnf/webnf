(defproject webnf/base "0.2.0-alpha1"
  :description "Collection org.clojure libs and essential others"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.clojure/clojure "_" :upgrade false]
                 [webnf.deps/logback "_" :upgrade false]
                 ;; a few essential contribs
                 [org.clojure/core.async "_" :exclusions [org.clojure/tools.analyzer.jvm] :upgrade false]
                 [org.clojure/tools.analyzer.jvm "_" :upgrade false]
                 [org.clojure/core.match "_" :upgrade false]
                 [org.clojure/core.typed "_" :classifier "slim" :upgrade false]
                 [org.clojure/core.unify "_" :upgrade false]
                 [org.clojure/tools.logging "_" :upgrade false]
                 [org.clojure/tools.nrepl "_" :upgrade false]]
  :profiles
  {:dev {:dependencies [[org.clojure/clojurescript "_" :upgrade false]
                        [webnf.deps/dev "_" :upgrade false]]}})
