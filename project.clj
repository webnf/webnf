(defproject webnf/parent "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.9"]]

  :profiles {:provided
             {:dependencies [[org.clojure/clojure "_"]]}
             :dist
             {:modules {:dirs ["../dist"]}}

             :fast
             {:modules {:subprocess false}}}

  :modules  {:inherited
             {:url "http://github.com/webnf/webnf"
              :license {:name "Eclipse Public License"
                        :url "http://www.eclipse.org/legal/epl-v10.html"}
              :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                               :username "fetch" :password :gpg}]]
              :aliases      {"all" ^:displace ["do" "clean," "test," "install"]
                             "-f" ["with-profile" "+fast"]}
              :scm          {:dir ".."}}
             :dirs ["deps.logback" "deps.universe" "deps.dev" "deps.web" "deps.contrib"
                    "async-servlet" "base" "cljs" "datomic" "enlive.clj" "enlive.cljs" "handler" "server"]
             :versions {org.clojure/clojure           "1.6.0"
                        webnf                         "0.1.0-SNAPSHOT"
                        webnf.deps                    "0.1.0-SNAPSHOT"
                        webnf/async-servlet           "0.1.4-SNAPSHOT"
                        webnf.deps/logback            "0.1.0-alpha1"
                        webnf/base                    "0.1.0-alpha1"
                        webnf/datomic                 "0.1.0-alpha1"}})
