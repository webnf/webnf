(defproject webnf/base "0.1.17"
  :description "Collection org.clojure libs and essential others"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.clojure/clojure "_"]
                 [webnf.deps/logback "_"]
                 ;; a few essential contribs
                 [org.clojure/core.async "_"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/core.typed "_" :classifier "slim"]
                 [org.clojure/core.unify "0.5.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.nrepl "0.2.10"]])
