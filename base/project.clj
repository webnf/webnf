(defproject webnf/base "0.1.0-SNAPSHOT"
  :description "Collection org.clojure libs and essential others"
  :plugins [[lein-modules "0.3.10"]]
  :dependencies [[org.clojure/clojure "_"]
                 [webnf.deps/logback "_"]
                 ;; a few essential contribs
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.2.2"]
                 [org.clojure/core.typed "0.2.78"]
                 [org.clojure/core.unify "0.5.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.nrepl "0.2.7"]])
