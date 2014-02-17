(defproject webnf/handler "0.0.8"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                   :username "fetch" :password :gpg}]]
  :source-paths ["src/clj"]
  :dependencies [[webnf/base "0.0.9"]
                 [webnf/async-servlet "0.1.1"]
                 
                 [enlive "1.1.5"]
                 [net.cgrand/moustache "1.1.0"
                  :exclusions [ring/ring-core]]
                 [ring/ring-core "1.2.1"]
                 [ring-mock "0.1.5"]

                 [com.lambdaworks/scrypt "1.4.0"]
                 [nl.bitwalker/UserAgentUtils "1.2.4"]])
