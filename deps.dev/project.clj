(defproject webnf.deps/dev "0.1.16"
  :plugins [[lein-modules "0.3.11"]]
  :description "Development dependencies"
  :dependencies [[clj-stacktrace "0.2.8"]
                 [clojure-complete "0.2.4"]
                 [criterium "0.4.3"]
                 [lein-light-nrepl "0.1.0"]
                 [org.clojure/tools.reader "_"] ; override reader from lein-light-repl
                 [cider/cider-nrepl "0.9.1"]
                 [com.cemerick/piggieback "0.2.1"]
                 [weasel "0.7.0"]
                 [figwheel "0.3.7"]
                 [spyscope "0.1.5"]
                 [debugger "0.1.7"]
                 [compliment "0.2.4"]
                 [cljs-tooling "0.1.7"]
                 [debugger "0.1.7"]
                 [cljfmt "0.1.11"]])
