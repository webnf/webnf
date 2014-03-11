(defproject webnf.deps/core "0.0.2"
  :description "Just the basic clojure runtime + logging infrastructure. Contains a logback.xml"
  :url "http://github.com/webnf/webnf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.1.1"]
                 [org.slf4j/log4j-over-slf4j "1.7.6"]
                 [org.slf4j/jcl-over-slf4j "1.7.6"]
                 [org.slf4j/jul-to-slf4j "1.7.6"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]])
