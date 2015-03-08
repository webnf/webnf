(defproject webnf.deps/dev "0.1.0-alpha9"
  :plugins [[lein-modules "0.3.11"]]
  :description "Development dependencies"
  :dependencies [[clj-stacktrace "0.2.8"]
                 [clojure-complete "0.2.4"]
                 [criterium "0.4.3"]
                 [lein-light-nrepl "0.1.0"]
                 [org.clojure/tools.reader "0.8.16"] ; override reader from lein-light-repl
                 [cider/cider-nrepl "0.8.2"]
                 [com.cemerick/piggieback "0.1.5"]
                 [weasel "0.6.0"]
                 [figwheel "0.2.5-20150218.182942-2"]
                 [spyscope "0.1.5"]])
