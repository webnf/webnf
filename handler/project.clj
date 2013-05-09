(defproject webnf/handler "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clj"]
  :dependencies [[webnf/base "0.1.0-SNAPSHOT"]
                 [webnf/async-servlet "0.0.1"]
                 
                 [enlive "1.1.1"]
                 [net.cgrand/moustache "1.1.0"]
                 [net.bendlas.ring/ring-core "1.2.0-bendlas-beta2"]
                 [ring-mock "0.1.3"]

                 [org.mindrot/jbcrypt "0.3m"]
                 [nl.bitwalker/UserAgentUtils "1.2.4"]])
