(defproject webnf/datomic "0.1.0-alpha1"
  :plugins [[lein-modules "0.3.9"]]
  :description "Missing datomic pieces"
  :dependencies [[webnf/base "_"]]
  :profiles {:provided {:dependencies [[com.datomic/datomic-free "0.9.4894"
                                        :exclusions [org.slf4j/slf4j-nop]]]}})
