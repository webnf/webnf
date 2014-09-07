(defproject webnf/enlive.clj "0.0.4-SNAPSHOT"
  :description "Helpers and wrappers to make enlive even more versatile
  - provides alternate version of load-html, that caches on last-modified to provide dynamic recompilation
  - provides alternate versions of deftemplate and defsnippet to reload (cached) on every invokation"
  :url "http://github.com/webnf/webnf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha2"]
                 [webnf/base "0.0.12-SNAPSHOT"]
                 [enlive "1.1.5"]
                 [org.clojure/core.cache "0.6.4"]])
