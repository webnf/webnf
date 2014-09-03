(defproject webnf/datomic "0.1.0-SNAPSHOT"
  :description "Missing datomic pieces"
  :url "http://github.com/webnf/webnf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[webnf/base "0.0.12-SNAPSHOT"]
                 [com.datomic/datomic-free "0.9.4894"
                  :exclusions [org.slf4j/slf4j-nop]]
                 [org.clojure/algo.monads "0.1.5"]])
