(defproject webnf/mui "0.2.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]
            [lein-cljsbuild "1.1.3"]
            [lein-shell "0.5.0"]]
  :dependencies [[webnf/base "_" :upgrade false]
                 [org.clojure/clojurescript "_" :upgrade false]
                 [rum "0.9.0" :exclusions [cljsjs/react cljsjs/react-dom]]]
  :prep-tasks [["shell" "npm" "--prefix" "npm" "install"]
               ["shell" "npm" "--prefix" "npm" "run" "dist"]]
  :clean-targets ^{:protect false} [:target-path "npm/node_modules"]
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
