(defproject webnf/handler "0.2.0-alpha1"
  :plugins [[lein-modules "0.3.11"]]
  :description "The first minor release of webnf handler, a collection
  of code to aid web handler development. There is:
  - Browser middlewares
    - to allow http method and header overrides from a browser form or uri
    - for extremely pretty exception printing, with source locations
    - user agent parsing
  - Timer helpers
  - Common dependencies
  - Dataflow / Validation"
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "_" :upgrade false]
                 [webnf/base "_" :upgrade false]
                 [webnf/async-servlet "_" :upgrade false]
                 [webnf.deps/web "_" :upgrade false]
                 [webnf/enlive "_" :upgrade false]
                 [com.lambdaworks/scrypt "1.4.0"]])
