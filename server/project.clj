(defproject webnf/server "0.0.10"
  :description "Vhost functionality with servlets and a jetty runner"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                   :username "fetch" :password :gpg}]]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [webnf/async-servlet "0.1.2"]
                 [webnf/base "0.0.10"]
                 [webnf.deps/web "0.0.2"]
                 [ring/ring-servlet "1.2.1"
                  :exclusions [javax.servlet/servlet-api]]

                 [org.eclipse.jetty/jetty-servlet "9.1.3.v20140225"]
                 [org.eclipse.jetty/jetty-webapp "9.1.3.v20140225"]
                 [org.jfastcgi.client/client-servlet "2.3"]])
