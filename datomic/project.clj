(defproject webnf/datomic "0.2.0-alpha1"
  :plugins [[lein-modules "0.3.11"]]
  :description "Missing datomic pieces"
  :dependencies [[webnf/base "_" :upgrade false]
                 [com.datomic/datomic-free "0.9.5544"
                  :exclusions [org.slf4j/slf4j-nop joda-time]]
                 [joda-time "_" :upgrade false]
                 [com.stuartsierra/component "_" :upgrade false]])
