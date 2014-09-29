(defproject webnf/base "0.0.12-SNAPSHOT"
  :description "Collection org.clojure libs and essential others"
  :url "http://github.com/webnf/webnf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                   :username "fetch" :password :gpg}]]
  :dependencies [[org.clojure/clojure "1.7.0-alpha2"]
                 [webnf.deps/core "0.0.5-SNAPSHOT"]
                 ;; a few essential contribs
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.2.2"]
                 [org.clojure/tools.nrepl "0.2.6"]])
