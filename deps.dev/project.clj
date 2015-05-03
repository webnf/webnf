(defproject webnf.deps/dev "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Development dependencies"
  :dependencies [[clj-stacktrace "0.2.8"]
                 [clojure-complete "0.2.4"]
                 [criterium "0.4.3"]
                 [lein-light-nrepl "0.1.0"]
                 [org.clojure/tools.reader "0.9.1"] ; override reader from lein-light-repl
                 [cider/cider-nrepl "0.9.0-20150429.131220-31"]
                 [com.cemerick/piggieback "0.2.1"]
                 #_[com.cemerick/piggieback "0.1.5"]
                 #_[weasel "0.6.0"]
                 [figwheel "0.2.7"]
                 [spyscope "0.1.5"]])
