(defproject webnf/async-servlet "0.1.4-SNAPSHOT"
  :description "A servlet 3.0 implementation, that enables the async api for ring applications"
  :url "http://github.com/webnf/webnf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                   :username "fetch" :password :gpg}]]
  :java-source-paths ["src/jvm"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [webnf.deps/core "0.0.4"]
                 [ring/ring-servlet "1.3.0"
                  :exclusions [javax.servlet/servlet-api]]
                 [javax.servlet/javax.servlet-api "3.1.0" :scope "provided"]]

  :profiles {:test {:dependencies [[webnf/server "0.0.12-SNAPSHOT"]]}})
