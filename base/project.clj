(defproject webnf/base "0.2.0-alpha2"
  :description "Collection org.clojure libs and essential others"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [webnf.deps/logback "0.2.0-alpha1"]
                 ;; a few essential contribs
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/core.async "0.3.441" :exclusions [org.clojure/tools.analyzer.jvm]]
                 [org.clojure/tools.analyzer.jvm "0.7.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/core.typed "0.3.32" :classifier "slim"]
                 [org.clojure/core.unify "0.5.7"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.nrepl "0.2.12"]]
  :profiles
  {:dev {:dependencies [[org.clojure/clojurescript "1.9.495"]
                        [webnf.deps/dev "_" :upgrade false]]}})
