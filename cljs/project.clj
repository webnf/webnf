(defproject webnf/cljs "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]
            [lein-cljsbuild "1.0.5"]]
  :description "ClojureScript functionality
  - port of strint from clojure.core.incubator
  - Event delecation helper in webnf.event"
  :dependencies [[org.clojure/clojurescript "_"]
                 [webnf/base "_"]
                 [bostonou/cljs-pprint "0.0.4-20150324.032505-1"]]
  :cljsbuild {:builds
              {:test
               {:source-paths ["src" "test"]
                :compiler {:optimizations :advanced
                           :output-to  "target/tests/main.js"
                           :output-dir "target/tests"
                           :source-map "target/tests/main.js.map"
                           :pretty-print true
                           :pseudo-names true
                           :target :nodejs}}}})
