(defproject webnf/handler "0.1.0-SNAPSHOT"
  :description "The first minor release of webnf handler, a collection
  of code to aid web handler development. There is:
  - Browser middlewares
    - to allow http method and header overrides from a browser form or uri
    - for extremely pretty exception printing, with source locations
    - user agent parsing
  - Timer helpers
  - Common dependencies"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bendlas-nexus" {:url "http://nexus.bendlas.net/content/groups/public"
                                   :username "fetch" :password :gpg}]]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [webnf/base "0.1.0-SNAPSHOT"]
                 [webnf/async-servlet "0.1.2-SNAPSHOT"]
                 [webnf.deps/net "0.1.0-SNAPSHOT"]])
