(defproject webnf.deps/dev "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Development dependencies"
  :dependencies [[clj-stacktrace "0.2.8"]
                 [clojure-complete "0.2.4"]
                 [criterium "0.4.3"]
                 [lein-light-nrepl "0.1.0"]
                 [org.clojure/tools.reader "0.8.16"] ; override reader from lein-light-repl
                 [cider/cider-nrepl "0.9.0-20150307.214951-14"]
                 [com.cemerick/piggieback "0.1.6-20150223.163620-3"]
                 [weasel "0.6.0-20150216.221131-3"]
                 [figwheel "0.2.5-20150218.182942-2"]
                 [spyscope "0.1.5"]])
