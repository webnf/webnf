(defproject webnf/handler "0.0.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clj"]
  :dependencies [[webnf/base "0.0.2"]
                 [webnf/async-servlet "0.0.1"]
                 
                 [enlive "1.1.0"]
                 [net.cgrand/moustache "1.1.0"
                  :exclusions [ring/ring-core]]
                 [ring/ring-core "1.2.0"]
                 [ring-mock "0.1.5"]

                 [org.mindrot/jbcrypt "0.3m"]
                 [nl.bitwalker/UserAgentUtils "1.2.4"]])
