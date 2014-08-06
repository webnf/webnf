(defproject webnf.deps/web "0.0.3"
  :description "Common dependencies for web apps"
  :url "http://github.com/webnf/webnf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[webnf.deps/core "0.0.4"]
                 [nl.bitwalker/UserAgentUtils "1.2.4"]
                 [net.sourceforge.cssparser/cssparser "0.9.14"]                 
                 [org.lesscss/lesscss "1.7.0.1.1"]
                 [commons-codec "1.9"]
                 [javax.mail/mail "1.4.7"]
                 [commons-net "3.3"]
                 [ring/ring-core "1.3.0"]
                 [ring-mock "0.1.5"]
                 [org.clojure/tools.nrepl "0.2.3"]])
