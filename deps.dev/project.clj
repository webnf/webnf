(defproject webnf.deps/dev "0.1.19-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Development dependencies"
  :dependencies [[webnf.deps/contrib "_" :upgrade false]
                 [clj-stacktrace "0.2.8"]
                 [clojure-complete "0.2.4"]
                 [criterium "0.4.4"]
                 [lein-light-nrepl "0.3.2"
                  :exclusions [ibdknox/tools.reader ;; conflics with o.c/t.r
                               org.clojure/tools.nrepl
                               org.clojure/clojurescript]]
                 [org.clojure/tools.reader "_" :upgrade false] ; override reader from lein-light-repl
                 [org.clojure/tools.nrepl "_" :upgrade false]
                 [cider/cider-nrepl "0.11.0"
                  :exclusions [org.clojure/tools.nrepl]]
                 [com.cemerick/piggieback "0.2.1" :exclusions [org.clojure/clojurescript]]
                 [weasel "0.7.0" :exclusions [org.clojure/clojurescript]]
                 [figwheel "0.5.0-6" :exclusions [org.clojure/clojurescript]]
                 [spyscope "0.1.5"]
                 [debugger "0.1.8"]
                 [compliment "0.2.7"]
                 [cljs-tooling "0.2.0"]
                 [cljfmt "0.4.1"]])
