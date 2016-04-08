(defproject webnf/mui "0.2.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]
            [lein-cljsbuild "1.1.3"]]
  :dependencies [[webnf/base "_" :upgrade false]
                 [org.clojure/clojurescript "_" :upgrade false]
                 [org.omcljs/om "1.0.0-alpha32"]
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
