(defproject webnf.handler/auth "0.2.0-alpha3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "A zero-auth provider, based on a custom protocol"
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [webnf.deps/logback "_" :upgrade false]
                 [webnf/base "_" :upgrade false]
                 [io.replikativ/hasch "0.3.4"]
                 [org.clojure/data.fressian "0.2.1"]
                 [ring/ring-core "1.6.1"]
                 [instaparse "1.4.7"]]
  :profiles
  {:dev {:dependencies [[ring/ring-mock "0.3.0"]
                        [org.clojure/test.check "0.10.0-alpha1"]]}})
