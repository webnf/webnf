(defproject webnf/parent "0.1.15"
  ;; lein -f modules change version leiningen.release/bump-version alpha
  :plugins [[lein-modules "0.3.11"]]

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
              :javac-options ["-source" "1.7" "-target" "1.7"]
              #_#_:repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                               :username "fetch" :password :gpg}]]
              :aliases      {"all" ^:displace ["do" "clean," "test," "install"]
                             "-f" ["with-profile" "+fast"]}
              :scm          {:dir ".."}}
             :dirs ["deps.logback" "deps.universe" "deps.dev" "deps.web" "deps.contrib"
                    "async-servlet" "base" "cljs" "datomic" "enlive.clj" "enlive.cljs"
                    "handler" "server" "filestore"]
             :versions {org.clojure/clojure              "1.7.0-RC1"
                        webnf                            "0.1.15"
                        webnf.deps                       "0.1.15"
                        ;;  is seriously broken
                        ;; "0.1.242.0-44b1e3-alpha"
                        org.clojure/core.async           "0.1.346.0-17112a-alpha"
                        org.clojure/clojurescript        "0.0-3308"
                        org.clojure/tools.reader         "0.10.0-alpha1"
                        org.clojure/core.typed           "0.3.0-alpha5"}})
