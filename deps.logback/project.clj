(defproject webnf.deps/logback "0.2.0-alpha1"
  :plugins [[lein-modules "0.3.11"]]
  :description "Basic slf4j logging config in form of a default logback.xml"
  :dependencies [[ch.qos.logback/logback-classic "1.1.8"]
                 [org.slf4j/slf4j-api "SLF4J" :upgrade false]
                 [org.slf4j/log4j-over-slf4j "SLF4J" :upgrade false]
                 [org.slf4j/jcl-over-slf4j "SLF4J" :upgrade false]
                 [org.slf4j/jul-to-slf4j "SLF4J" :upgrade false]])
