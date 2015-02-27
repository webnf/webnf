(defproject webnf/datomic "0.1.0-alpha8"
  :plugins [[lein-modules "0.3.10"]]
  :description "Missing datomic pieces"
  :dependencies [[webnf/base "_"]
                 [com.datomic/datomic-free "0.9.5130"
                  :exclusions [org.slf4j/slf4j-nop joda-time]]
                 [joda-time "2.7"]])
