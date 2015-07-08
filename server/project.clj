(defproject webnf/server "0.1.17"
  :plugins [[lein-modules "0.3.11"]]
  :description "Vhost functionality with servlets and a jetty runner"
  :java-source-paths ["src/jvm"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "_"]
                 [webnf/async-servlet "_"]
                 [webnf/base "_"]
                 [webnf.deps/web "_"]
                 
                 [org.eclipse.jetty/jetty-servlet "9.2.11.v20150529"]
                 [org.eclipse.jetty/jetty-webapp "9.2.11.v20150529"]
                 [org.eclipse.jetty.fcgi/fcgi-server "9.2.11.v20150529"]
                 [org.eclipse.jetty/jetty-alpn-server "9.2.11.v20150529"]
                 [com.stuartsierra/component "0.2.3"]])
