(defproject webnf.handler/auth "0.2.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "A zero-auth provider, based on a custom protocol"
  :dependencies [[org.clojure/clojure "_" :upgrade false]
                 [webnf.deps/logback "_" :upgrade false]
                 [webnf/base "_" :upgrade false]
                 [io.replikativ/hasch "0.3.0"]
                 [org.clojure/data.fressian "0.2.1"]
                 [ring/ring-core "_" :upgrade false]
                 [instaparse "_" :upgrade false]]
  :profiles
  {:dev {:dependencies [[ring/ring-mock "_" :upgrade false]
                        [org.clojure/test.check "_" :upgrade false]]}})
