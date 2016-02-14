(defproject webnf/cljs "0.1.19-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]
            [lein-cljsbuild "1.1.2"]]
  :description "ClojureScript functionality
  - port of strint from clojure.core.incubator
  - Event delecation helper in webnf.event"
  :dependencies [[webnf/base "_" :upgrade false]
                 [webnf.deps/contrib "_" :upgrade false]
                 [webnf.deps/web "_" :upgrade false]
                 [org.webjars/jszip "2.4.0"]
                 [org.omcljs/om "1.0.0-alpha30"]
                 [prismatic/om-tools "0.4.0"]]
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
