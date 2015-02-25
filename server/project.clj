(defproject webnf/server "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.10"]]
  :description "Vhost functionality with servlets and a jetty runner"
  :java-source-paths ["src/jvm"]
  :javac-options ["-source" "1.7"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "_"]
                 [webnf/async-servlet "_"]
                 [webnf/base "_"]
                 [webnf.deps/web "_"]
                 [ring/ring-servlet "1.3.2"
                  :exclusions [javax.servlet/servlet-api]]
                 
                 [org.eclipse.jetty/jetty-servlet "9.2.7.v20150116"]
                 [org.eclipse.jetty/jetty-webapp "9.2.7.v20150116"]
                 [org.eclipse.jetty.fcgi/fcgi-server "9.2.7.v20150116"]
                 [com.stuartsierra/component "0.2.2"]])
