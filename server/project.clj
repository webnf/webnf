(defproject webnf/server "0.0.5"
  :description "Vhost functionality with servlets and a jetty runner"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[webnf/base "0.0.4"]
                 [ring/ring-servlet "1.2.0"
                  :exclusions [javax.servlet/servlet-api]]
                 [org.eclipse.jetty/jetty-servlet "9.1.0.M0"]
                 [org.eclipse.jetty/jetty-webapp "9.1.0.M0"]
                 ;[org.eclipse.jetty.spdy/spdy-http-server "9.1.0.M0"]
                 ])
