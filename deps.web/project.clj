(defproject webnf.deps/web "0.2.0-alpha3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Common dependencies for web apps"
  :url "http://github.com/webnf/webnf"
  :dependencies [[eu.bitwalker/UserAgentUtils "1.20"]
                 [net.sourceforge.cssparser/cssparser "0.9.23"]
                 [org.lesscss/lesscss "1.7.0.1.1"
                  :exclusions [org.slf4j/slf4j-simple
                               org.mozilla/rino]]
                 [org.mozilla/rhino "1.7.7.1"]
                 [commons-codec "1.10"]
                 [javax.mail/javax.mail-api "1.6.0-rc2"]
                 [com.sun.mail/javax.mail "1.6.0-rc2"]
                 [commons-net "3.6"]
                 [ring/ring-core "1.6.1"]
                 [ring-mock "0.1.5"]
                 [net.cgrand/moustache "1.2.0-alpha2"
                  :exclusions [ring/ring-core]]
                 [hiccup "2.0.0-alpha1"]
                 [liberator "0.14.1"]
                 [prone "1.1.4"]
                 [webnf.compat/yuicompressor "_" :upgrade false]
                 [garden "1.3.2"
                  :exclusions [com.yahoo.platform.yui/yuicompressor]]
                 [webnf/enlive "_" :upgrade false]
                 [camel-snake-kebab "0.4.0"]])
