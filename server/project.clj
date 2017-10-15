(defproject webnf/server "0.2.0-alpha3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Vhost functionality with servlets and a jetty runner"
  :java-source-paths ["src/jvm"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.9.0-beta2"]
                 [webnf/async-servlet "_" :upgrade false]
                 [webnf/base "_" :upgrade false]
                 [webnf.deps/web "_" :upgrade false]
                 
                 [org.eclipse.jetty/jetty-servlet "9.4.7.v20170914"]
                 [org.eclipse.jetty/jetty-webapp "9.4.7.v20170914"]
                 [org.eclipse.jetty.fcgi/fcgi-server "9.4.7.v20170914"]
                 [org.eclipse.jetty/jetty-alpn-server "9.4.7.v20170914"]
                 [org.eclipse.jetty/jetty-servlets "9.4.7.v20170914"]
                 [com.stuartsierra/component "0.3.2"]])
