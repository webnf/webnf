(defproject webnf/parent "0.1.0-alpha7"
  ;; lein -f modules change version leiningen.release/bump-version alpha
  :plugins [[lein-modules "0.3.10"]]

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
             :versions {org.clojure/clojure           "1.7.0-alpha5"
                        webnf                         "0.1.0-alpha7"
                        webnf.deps                    "0.1.0-alpha7"
                        webnf/async-servlet           "0.1.9"}})
