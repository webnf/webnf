(defproject webnf/base "0.0.9"
  :description "Collection org.clojure libs and essential others"
  :url "http://github.com/webnf/webnf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                   :username "fetch" :password :gpg}]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [webnf.deps/core "0.0.1"]])
