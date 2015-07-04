(defproject webnf/datomic "0.1.16"
  :plugins [[lein-modules "0.3.11"]]
  :description "Missing datomic pieces"
  :dependencies [[webnf/base "_"]
                 [com.datomic/datomic-free "0.9.5186"
                  :exclusions [org.slf4j/slf4j-nop joda-time]]
                 [joda-time "2.8.1"]])
