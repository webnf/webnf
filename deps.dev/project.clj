(defproject webnf.deps/dev "0.1.19-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Development dependencies"
  :dependencies [[clj-stacktrace "0.2.8"]
                 [clojure-complete "0.2.4"]
                 [criterium "0.4.3"]
                 [lein-light-nrepl "0.1.0"
                  :exclusions [ibdknox/tools.reader ;; conflics with o.c/t.r
                               org.clojure/tools.nrepl]]
                 [org.clojure/tools.reader "_"] ; override reader from lein-light-repl
                 [org.clojure/tools.nrepl "_"]
                 [cider/cider-nrepl "0.9.1"
                  :exclusions [org.clojure/tools.nrepl]]
                 [com.cemerick/piggieback "0.2.1"]
                 [weasel "0.7.0"]
                 [figwheel "0.3.7"]
                 [spyscope "0.1.5"]
                 [debugger "0.1.7"]
                 [compliment "0.2.4"]
                 [cljs-tooling "0.1.7"]
                 [debugger "0.1.7"]
                 [cljfmt "0.2.0"]])
