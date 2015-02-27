(defproject webnf/handler "0.1.0-alpha8"
  :plugins [[lein-modules "0.3.10"]]
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
  :dependencies [[org.clojure/clojure "_"]
                 [webnf/base "_"]
                 [webnf/async-servlet "_"]
                 [webnf.deps/web "_"]
                 [com.lambdaworks/scrypt "1.4.0"]])
