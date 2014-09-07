(defproject webnf/server "0.0.12-SNAPSHOT"
  :description "Vhost functionality with servlets and a jetty runner"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                   :username "fetch" :password :gpg}]]
  :java-source-paths ["src/jvm"]
  :javac-options ["-source" "1.7"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.7.0-alpha2"]
                 [webnf/async-servlet "0.1.4-SNAPSHOT"]
                 [webnf/base "0.0.12-SNAPSHOT"]
                 [webnf.deps/web "0.0.4-SNAPSHOT"]
                 [ring/ring-servlet "1.3.1"
                  :exclusions [javax.servlet/servlet-api]]

                 [org.eclipse.jetty/jetty-servlet "9.2.2.v20140723"]
                 [org.eclipse.jetty/jetty-webapp "9.2.2.v20140723"]
                 [org.eclipse.jetty.fcgi/fcgi-server "9.2.2.v20140723"]
                 [com.stuartsierra/component "0.2.2"]])
