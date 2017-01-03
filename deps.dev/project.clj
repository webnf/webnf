(defproject webnf.deps/dev "0.2.0-SNAPSHOT"
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
                 [org.clojure/tools.reader "_" :upgrade false] ; override reader from lein-light-repl
                 [org.clojure/tools.nrepl "_" :upgrade false]
                 [cider/cider-nrepl "0.14.0"
                  :exclusions [org.clojure/tools.nrepl]]
                 [com.cemerick/piggieback "0.2.1" :exclusions [org.clojure/clojurescript]]
                 [weasel "0.7.0" :exclusions [org.clojure/clojurescript]]
                 [ring "_" :upgrade false]
                 [figwheel-sidecar "0.5.8" :exclusions [org.clojure/clojurescript]]
                 [figwheel-sidecar "0.5.8"]
                 [binaryage/devtools "0.8.3"]
                 [spyscope "0.1.6"]
                 [debugger "0.2.0"]
                 [compliment "0.3.2"]
                 [cljs-tooling "0.2.0"]
                 [cljfmt "0.5.6"]])
