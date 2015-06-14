(defproject webnf.deps/web "0.1.15"
  :plugins [[lein-modules "0.3.11"]]
  :description "Common dependencies for web apps"
  :url "http://github.com/webnf/webnf"
  :dependencies [[eu.bitwalker/UserAgentUtils "1.15"]
                 [net.sourceforge.cssparser/cssparser "0.9.16"]
                 [org.lesscss/lesscss "1.7.0.1.1"
                  :exclusions [org.slf4j/slf4j-simple]]
                 [commons-codec "1.10"]
                 [com.sun.mail/javax.mail "1.5.3"]
                 [commons-net "3.3"]
                 [ring/ring-core "1.4.0-RC1"]
                 [ring-mock "0.1.5"]
                 [net.cgrand/moustache "1.2.0-alpha2"
                  :exclusions [ring/ring-core]]
                 [hiccup "1.0.5"]
                 [liberator "0.13"]
                 [prone "0.8.2"]
                 [garden "1.2.6"]])
