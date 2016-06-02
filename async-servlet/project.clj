(defproject webnf/async-servlet "0.2.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "A servlet 3.0 implementation, that enables the async api for ring applications"
  :java-source-paths ["src/jvm"]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "_" :upgrade false]
                 [webnf.deps/logback "_" :upgrade false]
                 [webnf/base "_" :upgrade false]
                 [ring/ring-servlet "1.5.0-RC1"
                  :exclusions [javax.servlet/servlet-api]]
                 [javax.servlet/javax.servlet-api "_" :scope "provided" :upgrade false]]
  :aot [webnf.async-servlet.UpgradeHandler]
  :profiles {:test {:dependencies [[webnf/server "_" :upgrade false]
                                   [clj-http "_" :upgrade false]]}})
