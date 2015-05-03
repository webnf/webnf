(defproject webnf/server "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Vhost functionality with servlets and a jetty runner"
  :java-source-paths ["src/jvm"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "_"]
                 [webnf/async-servlet "_"]
                 [webnf/base "_"]
                 [webnf.deps/web "_"]
                 [ring/ring-servlet "1.3.2"
                  :exclusions [javax.servlet/servlet-api]]
                 
                 [org.eclipse.jetty/jetty-servlet "9.2.10.v20150310"]
                 [org.eclipse.jetty/jetty-webapp "9.2.10.v20150310"]
                 [org.eclipse.jetty.fcgi/fcgi-server "9.2.10.v20150310"]
                 [org.eclipse.jetty/jetty-alpn-server "9.2.10.v20150310"]
                 [com.stuartsierra/component "0.2.3"]])
