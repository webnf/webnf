(defproject webnf/async-servlet "0.2.0-alpha2"
  :plugins [[lein-modules "0.3.11"]]
  :description "A servlet 3.0 implementation, that enables the async api for ring applications"
  :java-source-paths ["src/jvm"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [webnf.deps/logback "0.2.0-alpha1"]
                 [webnf/base "_" :upgrade false]
                 [ring/ring-servlet "1.6.0-beta7"
                  :exclusions [javax.servlet/servlet-api]]
                 [javax.servlet/javax.servlet-api "4.0.0-b03" :scope "provided"]]
  :aot [webnf.async-servlet.UpgradeHandler]
  :profiles {:test {:dependencies [[webnf/server "_" :upgrade false]
                                   [clj-http "3.4.1"]]}})
