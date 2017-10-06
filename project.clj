(defproject webnf/parent "0.2.0-alpha3-SNAPSHOT"
  ;; lein -f modules change version leiningen.release/bump-version alpha
  :plugins [[lein-modules "0.3.11"]]

  :profiles {:provided
             {:dependencies [[org.clojure/clojure "1.9.0-beta1"]]}
             
             :dist
             {:modules {:dirs ["../dist"]}}

             :fast
             {:modules {:subprocess false}}}

  :modules  {:inherited
             {:url "http://github.com/webnf/webnf"
              :license {:name "Eclipse Public License"
                        :url "http://www.eclipse.org/legal/epl-v10.html"}
              :javac-options ["-source" "1.7" "-target" "1.7"]
              :aliases      {"all" ^:displace ["do" "clean," "test," "install"]
                             "-f" ["with-profile" "+fast"]}
              :scm          {:dir ".."}}
             :dirs ["deps.logback" "deps.universe" "deps.dev" "deps.web" "deps.contrib"
                    "async-servlet" "base" "datomic" "enlive" "davstore"
                    ;; "compat.yuicompressor"
                    "handler" "server" "filestore" "cats" "js" "handler.auth"]
             :versions {webnf                            "0.2.0-alpha3-SNAPSHOT"
                        webnf.deps                       "0.2.0-alpha3-SNAPSHOT"
                        webnf.handler                    "0.2.0-alpha3-SNAPSHOT"
                        webnf.compat/yuicompressor       "2.4.8"
                        ring                             "1.5.0"
                        ring/ring-mock                   "0.3.1"}})
