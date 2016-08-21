(defproject webnf.deps/dev "0.2.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Development dependencies"
  :dependencies [[webnf.deps/contrib "_" :upgrade false]
                 [webnf/server "_" :upgrade false]
                 [clj-stacktrace "0.2.8"]
                 [clojure-complete "0.2.4"]
                 [criterium "0.4.4"]
                 [lein-light-nrepl "0.3.3"
                  :exclusions [ibdknox/tools.reader ;; conflics with o.c/t.r
                               org.clojure/tools.nrepl
                               org.clojure/clojurescript]]
                 [org.clojure/tools.reader "_" :upgrade false] ; override reader from lein-light-repl
                 [org.clojure/tools.nrepl "_" :upgrade false]
                 [cider/cider-nrepl "0.13.0"
                  :exclusions [org.clojure/tools.nrepl]]
                 [com.cemerick/piggieback "0.2.1" :exclusions [org.clojure/clojurescript]]
                 [weasel "0.7.0" :exclusions [org.clojure/clojurescript]]
                 [figwheel-sidecar "0.5.4-7" :exclusions [org.clojure/clojurescript]]
                 [figwheel-sidecar "0.5.4-7"]
                 [binaryage/devtools "0.8.1"]
                 [ring "1.5.0"]
                 [spyscope "0.1.5"]
                 [debugger "0.2.0"]
                 [compliment "0.3.1"]
                 [cljs-tooling "0.2.0"]
                 [cljfmt "0.5.3"]])
