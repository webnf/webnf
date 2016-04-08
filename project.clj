(defproject webnf/parent "0.2.0-SNAPSHOT"
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
                    "async-servlet" "base" "datomic" "enlive"
                    "handler" "server" "filestore" "compat.yuicompressor" "cats" "js" "mui"]
             :versions {org.clojure/clojure              "1.8.0"
                        webnf                            "0.2.0-SNAPSHOT"
                        webnf.deps                       "0.2.0-SNAPSHOT"
                        webnf.compat/yuicompressor       "2.4.8"
                        org.clojure/core.async           "0.2.374"
                        org.clojure/clojurescript        "1.8.40"
                        org.clojure/tools.reader         "1.0.0-alpha4"
                        org.clojure/tools.nrepl          "0.2.12"
                        org.clojure/tools.analyzer.jvm   "0.6.9"
                        org.clojure/core.typed           "0.3.22"
                        org.clojure/core.match           "0.3.0-alpha4"
                        org.clojure/core.unify           "0.5.6"
                        org.clojure/tools.logging        "0.3.1"
                        org.mozilla/rhino                "1.7.7.1"
                        com.google.code.findbugs/jsr305  "3.0.1"
                        com.stuartsierra/component       "0.3.1"
                        joda-time                        "2.9.2"
                        javax.servlet/javax.servlet-api  "3.1.0"
                        "JETTY"                          "9.3.8.v20160314"
                        "LOGBACK"                        "1.7.18"
                        "MAIL-API"                       "1.5.5"}})
