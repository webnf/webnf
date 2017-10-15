(defproject webnf.deps/dev "0.2.0-alpha3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Development dependencies"
  :dependencies [[webnf.deps/contrib "_" :upgrade false]
                 [clj-stacktrace "0.2.8"]
                 [clojure-complete "0.2.4"]
                 [criterium "0.4.4"]
                 [lein-light-nrepl "0.3.3"
                  :exclusions [ibdknox/tools.reader ;; conflics with o.c/t.r
                               org.clojure/tools.nrepl
                               org.clojure/clojurescript]]
                 [org.clojure/tools.reader "1.1.0"] ; override reader from lein-light-repl
                 [org.clojure/tools.nrepl "0.2.13"]
                 [cider/cider-nrepl "0.15.1"
                  :exclusions [org.clojure/tools.nrepl]]
                 [com.cemerick/piggieback "0.2.2" :exclusions [org.clojure/clojurescript]]
                 [weasel "0.7.0" :exclusions [org.clojure/clojurescript]]
                 [ring "1.6.2"]
                 [figwheel-sidecar "0.5.14" :exclusions [org.clojure/clojurescript]]
                 [binaryage/devtools "0.9.7"]
                 [spyscope "0.1.6"]
                 [debugger "0.2.0"]
                 [compliment "0.3.4"]
                 [cljs-tooling "0.2.0"]
                 [cljfmt "0.5.7"]])
