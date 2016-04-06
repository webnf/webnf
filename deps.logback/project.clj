(defproject webnf.deps/logback "0.2.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Basic slf4j logging config in form of a default logback.xml"
  :dependencies [[ch.qos.logback/logback-classic "1.1.6"]
                 [org.slf4j/slf4j-api "LOGBACK" :upgrade false]
                 [org.slf4j/log4j-over-slf4j "LOGBACK" :upgrade false]
                 [org.slf4j/jcl-over-slf4j "LOGBACK" :upgrade false]
                 [org.slf4j/jul-to-slf4j "LOGBACK" :upgrade false]])
