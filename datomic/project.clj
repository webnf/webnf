(defproject webnf/datomic "0.2.0-alpha3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Missing datomic pieces"
  :dependencies [[webnf/base "_" :upgrade false]
                 [com.datomic/datomic-free "0.9.5656"
                  :exclusions [org.slf4j/slf4j-nop joda-time]]
                 [joda-time "2.9.9"]
                 [com.stuartsierra/component "0.3.2"]])
