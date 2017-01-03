(defproject webnf/server "0.2.0-alpha1"
  :plugins [[lein-modules "0.3.11"]]
  :description "Vhost functionality with servlets and a jetty runner"
  :java-source-paths ["src/jvm"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "_" :upgrade false]
                 [webnf/async-servlet "_" :upgrade false]
                 [webnf/base "_" :upgrade false]
                 [webnf.deps/web "_" :upgrade false]
                 
                 [org.eclipse.jetty/jetty-servlet "JETTY" :upgrade false]
                 [org.eclipse.jetty/jetty-webapp "JETTY" :upgrade false]
                 [org.eclipse.jetty.fcgi/fcgi-server "JETTY" :upgrade false]
                 [org.eclipse.jetty/jetty-alpn-server "JETTY" :upgrade false]
                 [org.eclipse.jetty/jetty-servlets "JETTY" :upgrade false]
                 [com.stuartsierra/component "_" :upgrade false]])
