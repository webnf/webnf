(defproject webnf/parent "0.1.19-SNAPSHOT"
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
                    "handler" "server" "filestore" "server.undertow"
                    "compat.yuicompressor" "cats"]
             :versions {org.clojure/clojure              "1.8.0"
                        webnf                            "0.1.19-SNAPSHOT"
                        webnf.deps                       "0.1.19-SNAPSHOT"
                        webnf.compat/yuicompressor       "2.4.8"
                        org.clojure/core.async           "0.2.374"
                        org.clojure/clojurescript        "1.7.228"
                        org.clojure/tools.reader         "1.0.0-alpha3"
                        org.clojure/tools.nrepl          "0.2.12"
                        org.clojure/tools.analyzer.jvm   "0.6.9"
                        org.clojure/core.typed           "0.3.19"
                        org.mozilla/rhino                "1.7.7"
                        com.google.code.findbugs/jsr305  "3.0.1"
                        com.stuartsierra/component       "0.3.1"
                        joda-time                        "2.9.1"
                        javax.servlet/javax.servlet-api  "3.1.0"
                        "JETTY"                          "9.3.6.v20151106"
                        "LOGBACK"                        "1.7.13"
                        "MAIL-API"                       "1.5.5"}})
