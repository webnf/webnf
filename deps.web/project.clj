(defproject webnf.deps/web "0.1.19-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "Common dependencies for web apps"
  :url "http://github.com/webnf/webnf"
  :dependencies [[eu.bitwalker/UserAgentUtils "1.18"]
                 [net.sourceforge.cssparser/cssparser "0.9.18"]
                 [org.lesscss/lesscss "1.7.0.1.1"
                  :exclusions [org.slf4j/slf4j-simple
                               org.mozilla/rino]]
                 [org.mozilla/rhino "_" :upgrade false]
                 [commons-codec "1.10"]
                 [com.sun.mail/javax.mail "1.5.4"]
                 [commons-net "3.4"]
                 [ring/ring-core "1.4.0"]
                 [ring-mock "0.1.5"]
                 [net.cgrand/moustache "1.2.0-alpha2"
                  :exclusions [ring/ring-core]]
                 [hiccup "1.0.5"]
                 [liberator "0.14.0"]
                 [prone "0.8.2"]
                 [webnf.compat/yuicompressor "_" :upgrade false]
                 [garden "1.3.0" ;; 1.2.6 is botched
                  :exclusions [com.yahoo.platform.yui/yuicompressor]]])
