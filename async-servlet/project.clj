(defproject webnf/async-servlet "0.1.0"
  :description "A servlet 3.0 implementation, that enables the async api for ring applications"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                   :username "fetch" :password :gpg}]]
  :java-source-paths ["src/jvm"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ring/ring-servlet "1.2.1"
                  :exclusions [javax.servlet/servlet-api]]
                 [org.glassfish/javax.servlet "3.1.1" :scope "provided"]]

  :profiles {:test {:dependencies [[webnf/server "0.0.6"]]}})
