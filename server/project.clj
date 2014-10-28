(defproject webnf/server "0.1.0-alpha2"
  :plugins [[lein-modules "0.3.9"]]
  :description "Vhost functionality with servlets and a jetty runner"
  :java-source-paths ["src/jvm"]
  :javac-options ["-source" "1.7"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "_"]
                 [webnf/async-servlet "_"]
                 [webnf/base "_"]
                 [webnf.deps/web "_"]
                 [ring/ring-servlet "1.3.1"
                  :exclusions [javax.servlet/servlet-api]]

                 [org.eclipse.jetty/jetty-servlet "9.3.0.M0"]
                 [org.eclipse.jetty/jetty-webapp "9.3.0.M0"]
                 [org.eclipse.jetty.fcgi/fcgi-server "9.3.0.M0"]
                 [com.stuartsierra/component "0.2.2"]])
