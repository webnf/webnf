(defproject webnf.deps/web "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.10"]]
  :description "Common dependencies for web apps"
  :url "http://github.com/webnf/webnf"
  :dependencies [[nl.bitwalker/UserAgentUtils "1.2.4"]
                 [net.sourceforge.cssparser/cssparser "0.9.14"]                 
                 [org.lesscss/lesscss "1.7.0.1.1"
                  :exclusions [org.slf4j/slf4j-simple]]
                 [commons-codec "1.10"]
                 [javax.mail/javax.mail-api "1.5.2"]
                 [commons-net "3.3"]
                 [ring/ring-core "1.3.2"]
                 [ring-mock "0.1.5"]
                 [net.cgrand/moustache "1.2.0-alpha2"
                  :exclusions [ring/ring-core]]
                 [hiccup "1.0.5"]
                 [liberator "0.12.2"]
                 [prone "0.8.1"]])
