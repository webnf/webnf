(defproject webnf/async-servlet "0.1.14"
  :plugins [[lein-modules "0.3.11"]]
  :description "A servlet 3.0 implementation, that enables the async api for ring applications"
  :java-source-paths ["src/jvm"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "_"]
                 [webnf.deps/logback "_"]
                 [webnf/base "_"]
                 [ring/ring-servlet "1.3.2"
                  :exclusions [javax.servlet/servlet-api]]
                 [javax.servlet/javax.servlet-api "3.1.0" :scope "provided"]]

  :profiles {:test {:dependencies [[webnf/server "_"]]}})
