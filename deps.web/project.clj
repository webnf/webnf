(defproject webnf.deps/web "0.2.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Common dependencies for web apps"
  :url "http://github.com/webnf/webnf"
  :dependencies [[eu.bitwalker/UserAgentUtils "1.20"]
                 [net.sourceforge.cssparser/cssparser "0.9.20"]
                 [org.lesscss/lesscss "1.7.0.1.1"
                  :exclusions [org.slf4j/slf4j-simple
                               org.mozilla/rino]]
                 [org.mozilla/rhino "_" :upgrade false]
                 [commons-codec "1.10"]
                 [javax.mail/javax.mail-api "MAIL-API" :upgrade false]
                 [com.sun.mail/javax.mail "MAIL-API" :upgrade false]
                 [commons-net "3.5"]
                 [ring/ring-core "_" :upgrade false]
                 [ring-mock "0.1.5"]
                 [net.cgrand/moustache "1.2.0-alpha2"
                  :exclusions [ring/ring-core]]
                 [hiccup "1.0.5"]
                 [liberator "0.14.1"]
                 [prone "1.1.2"]
                 [webnf.compat/yuicompressor "_" :upgrade false]
                 [garden "1.3.2"
                  :exclusions [com.yahoo.platform.yui/yuicompressor]]
                 [http-kit "2.2.0"]])
