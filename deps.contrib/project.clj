(defproject webnf.deps/contrib "0.2.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Depend on projects covered by clojure's CA"
  :dependencies [[org.clojure/algo.generic "0.1.2"]
                 [org.clojure/algo.monads "0.1.6"]
                 [com.google.code.findbugs/jsr305  "_" :upgrade false]
                 [org.clojure/clojurescript "_"
                  :exclusions [org.clojure/tools.reader
                               com.google.guava/guava
                               org.mozilla/rhino]
                  :upgrade false]
                 [com.google.guava/guava "21.0-rc1"]
                 [org.mozilla/rhino "_" :upgrade false]
                 [org.clojure/core.async "_" :exclusions [org.clojure/tools.analyzer.jvm] :upgrade false]
                 [org.clojure/core.cache "_" :upgrade false]
                 [org.clojure/core.contracts "0.0.6"]
                 [org.clojure/core.incubator "0.1.4"]
                 [org.clojure/core.logic "0.8.11"]
                 [org.clojure/core.match "_" :upgrade false]
                 [org.clojure/core.memoize "0.5.9" :exclusions [org.clojure/core.cache]]
                 [org.clojure/core.rrb-vector "0.0.11"]
                 [org.clojure/core.typed "_" :classifier "slim" :upgrade false]
                 [org.clojure/core.unify "_" :upgrade false]
                 [org.clojure/data.avl "0.0.17"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/data.finger-tree "0.0.2"]
                 [org.clojure/data.fressian "0.2.1"]
                 [org.clojure/data.generators "0.1.2"]
                 [org.clojure/data.int-map "0.2.4"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.priority-map "0.0.7"]
                 [org.clojure/data.xml "_" :upgrade false]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/java.classpath "0.2.3"]
                 [org.clojure/java.data "0.1.1"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.clojure/java.jmx "0.3.3"]
                 [org.clojure/jvm.tools.analyzer "0.6.1"]
                 [org.clojure/math.combinatorics "0.1.3"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/test.check "_" :upgrade false]
                 [org.clojure/test.generative "0.5.2"]
                 [org.clojure/tools.analyzer "0.6.9"]
                 [org.clojure/tools.analyzer.js "0.1.0-beta5" :exclusions [org.clojure/clojurescript]]
                 [org.clojure/tools.analyzer.jvm "_" :upgrade false]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.emitter.jvm "0.1.0-beta5"]
                 [org.clojure/tools.logging "_" :upgrade false]
                 [org.clojure/tools.macro "0.1.5"]
                 [org.clojure/tools.namespace "0.3.0-alpha3"]
                 [org.clojure/tools.nrepl "_" :upgrade false]
                 [org.clojure/tools.reader "_" :upgrade false]
                 [org.clojure/tools.trace "0.7.9"]])
