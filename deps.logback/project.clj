(defproject webnf.deps/logback "0.2.0-alpha3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Basic slf4j logging config in form of a default logback.xml"
  :dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                 [org.slf4j/slf4j-api "1.8.0-alpha2"]
                 [org.slf4j/log4j-over-slf4j "1.8.0-alpha2"]
                 [org.slf4j/jcl-over-slf4j "1.8.0-alpha2"]
                 [org.slf4j/jul-to-slf4j "1.8.0-alpha2"]])
