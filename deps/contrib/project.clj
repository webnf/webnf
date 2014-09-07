(defproject webnf.deps/contrib "0.0.4-SNAPSHOT"
  :description "Depend on projects covered by clojure's CA"
  :url "http://github.com/webnf/webnf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/core.incubator "0.1.3"]
                 [org.clojure/core.logic "0.8.8"]
                 [org.clojure/core.cache "0.6.3"]
                 [org.clojure/core.memoize "0.5.6"
                  :exclusions [org.clojure/core.cache]]
                 [org.clojure/core.contracts "0.0.5"]
                 [org.clojure/core.unify "0.5.6"]
                 [org.clojure/core.typed "0.2.65"]
                 [org.clojure/tools.reader "0.8.5"]
                 [org.clojure/tools.trace "0.7.8"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/data.generators "0.1.2"]
                 [org.clojure/java.classpath "0.2.2"]

                 [org.clojure/core.match "0.2.1"]

                 [org.clojure/tools.macro "0.1.5"]

                 [org.clojure/data.xml "0.0.7"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/data.priority-map "0.0.5"]
                 [org.clojure/data.finger-tree "0.0.2"]

                 [org.clojure/algo.monads "0.1.5"]
                 [org.clojure/algo.generic "0.1.2"]

                 [org.clojure/java.data "0.1.1"]
                 [org.clojure/java.jmx "0.3.0"]
                 [org.clojure/java.jdbc "0.3.5"]

                 [org.clojure/math.combinatorics "0.0.8"]
                 [org.clojure/math.numeric-tower "0.0.4"]])
