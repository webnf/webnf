(defproject webnf.handler/auth "0.2.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "A zero-auth provider, based on a custom protocol"
  :dependencies [[org.clojure/clojure "_" :upgrade false]
                 [webnf.deps/logback "_" :upgrade false]
                 [webnf/base "_" :upgrade false]
                 [io.replikativ/hasch "0.3.0"]
                 [org.clojure/data.fressian "0.2.1"]]
  :profiles {:test {:dependencies [[ring/ring-mock "0.3.0"]]}})
