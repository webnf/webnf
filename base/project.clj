(defproject webnf/base "0.0.5"
  :description "Collection org.clojure libs and essential others"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                   :username "fetch" :password :gpg}]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1978"]
                 [org.clojure/core.incubator "0.1.3"]
                 [org.clojure/core.logic "0.8.4"]
                 [org.clojure/core.cache "0.6.3"]
                 [org.clojure/core.memoize "0.5.6"
                  :exclusions [org.clojure/core.cache]]
                 [org.clojure/core.contracts "0.0.5"]
                 [org.clojure/core.unify "0.5.6"]
                 [org.clojure/core.typed "0.2.15"]
                 [org.clojure/tools.reader "0.7.10"]
                 [org.clojure/tools.trace "0.7.6"]
                 [org.clojure/tools.cli "0.2.4"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/data.generators "0.1.2"]
                 [org.clojure/java.classpath "0.2.1"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]

                 [org.clojure/core.match "0.2.0"]

                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [org.clojure/tools.macro "0.1.5"]

                 [org.clojure/data.xml "0.0.7"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/data.priority-map "0.0.3"]
                 [org.clojure/data.finger-tree "0.0.1"]

                 [org.clojure/algo.monads "0.1.4"]
                 [org.clojure/algo.generic "0.1.1"]

                 [org.clojure/java.data "0.1.1"]
                 [org.clojure/java.jmx "0.2.0"]
                 [org.clojure/java.jdbc "0.3.0-alpha3"]

                 [org.clojure/math.combinatorics "0.0.4"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 
                 [clj-stacktrace "0.2.7"]
                 [clojure-complete "0.2.3"]
                 [criterium "0.4.2"]

                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.slf4j/log4j-over-slf4j "1.7.5"]
                 [org.slf4j/jcl-over-slf4j "1.7.5"]
                 [org.slf4j/jul-to-slf4j "1.7.5"]

                 [clj-stacktrace "0.2.7"]
                 [clojure-complete "0.2.3"]
                 [criterium "0.4.2"]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.slf4j/log4j-over-slf4j "1.7.5"]
                 [org.slf4j/jcl-over-slf4j "1.7.5"]
                 [org.slf4j/jul-to-slf4j "1.7.5"]
                 
                 [net.intensivesystems/conduit "0.9.0"]
                 [net.sourceforge.cssparser/cssparser "0.9.11"]

                 [commons-codec "1.8"]
                 [javax.mail/mail "1.4.7"]
                 [clj-time "0.6.0"]])

