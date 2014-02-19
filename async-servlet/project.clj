(defproject webnf/async-servlet "0.1.2"
  :description "A servlet 3.0 implementation, that enables the async api for ring applications"
  :url "http://github.com/webnf/webnf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                   :username "fetch" :password :gpg}]]
  :java-source-paths ["src/jvm"]
  :source-paths ["src/clj"]
  :dependencies [[webnf.deps/core "0.0.1"]
                 [ring/ring-servlet "1.2.1"
                  :exclusions [javax.servlet/servlet-api]]
                 [org.glassfish/javax.servlet "3.1.1" :scope "provided"]]

  :profiles {:test {:dependencies [[webnf/server "0.0.6"]]}})
