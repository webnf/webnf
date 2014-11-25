(defproject webnf.deps/dev "0.1.0-alpha4"
  :plugins [[lein-modules "0.3.9"]]
  :description "Development dependencies"
  :dependencies [[clj-stacktrace "0.2.8"]
                 [clojure-complete "0.2.4"]
                 [criterium "0.4.3"]
                 [lein-light-nrepl "0.1.0"]
                 [org.clojure/tools.reader "0.8.12"] ; override reader from lein-light-repl
                 [cider/cider-nrepl "0.8.1"]
                 [com.cemerick/piggieback "0.1.3"]
                 [weasel "0.4.2"]])
