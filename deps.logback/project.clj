(defproject webnf.deps/logback "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.9"]]
  :description "Basic slf4j logging config in form of a default logback.xml"
  :dependencies [[ch.qos.logback/logback-classic "1.1.2"]
                 [org.slf4j/log4j-over-slf4j "1.7.7"]
                 [org.slf4j/jcl-over-slf4j "1.7.7"]
                 [org.slf4j/jul-to-slf4j "1.7.7"]])
