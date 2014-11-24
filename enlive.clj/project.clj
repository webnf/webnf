(defproject webnf/enlive.clj "0.1.0-alpha3"
  :plugins [[lein-modules "0.3.9"]]
  :description "Helpers and wrappers to make enlive even more versatile
  - provides alternate version of load-html, that caches on last-modified to provide dynamic recompilation
  - provides alternate versions of deftemplate and defsnippet to reload (cached) on every invokation"
  :dependencies [[org.clojure/clojure "_"]
                 [webnf/base "_"]
                 [enlive "1.1.5"]
                 [org.clojure/core.cache "0.6.4"]])
