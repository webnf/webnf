(defproject webnf/cljs "0.1.19-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]
            [lein-cljsbuild "1.0.6"]]
  :description "ClojureScript functionality
  - port of strint from clojure.core.incubator
  - Event delecation helper in webnf.event"
  :dependencies [[org.clojure/clojurescript "_"
                  :exclusions [org.clojure/tools.reader]]
                 [org.clojure/tools.reader "_"]
                 [webnf/base "_"]
                 [org.webjars/jszip "2.4.0"]]
  :cljsbuild {:builds
              {:test
               {:source-paths ["src" "test"]
                :libs ["src-js"]
                :compiler {:optimizations :advanced
                           :output-to  "target/tests/main.js"
                           :output-dir "target/tests"
                           :source-map "target/tests/main.js.map"
                           :pretty-print true
                           :pseudo-names true
                           :target :nodejs}}}})
