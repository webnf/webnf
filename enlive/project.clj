(defproject webnf/enlive "0.2.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Helpers and wrappers to make enlive even more versatile
  - provides alternate version of load-html, that caches on last-modified to provide dynamic recompilation
  - provides alternate versions of deftemplate and defsnippet to reload (cached) on every invokation"
  :dependencies [[org.clojure/clojure "_" :upgrade false]
                 [webnf/base "_" :upgrade false]
                 [enlive "1.1.6"]
                 [org.clojure/core.cache "_" :upgrade false]])
