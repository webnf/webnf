(ns webnf.test-user-agent
  (:use clojure.test webnf.user-agent)
  (:require webnf.test-util))

(deftest test-user-agent
  (are [s ua] (= (parse-user-agent s) ua)

       "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.43 Safari/537.31"
       {:operating-system {:name "Linux", :device-type "Computer", 
                           :group "Linux", :manufacturer "Other", :mobile false},
        :browser {:name "Chrome 26", :version "26.0.1410.43", :type "Browser", 
                  :manufacturer "Google Inc.", :rendering-engine "WEBKIT"}}

       "Mozilla/5.0 (X11; Linux x86_64; rv:17.0) Gecko/20100101 Firefox/17.0"
       {:operating-system {:name "Linux", :device-type "Computer", 
                           :group "Linux", :manufacturer "Other", :mobile false},
        :browser {:name "Firefox 17", :version "17.0", 
                  :type "Browser", :manufacturer "Mozilla Foundation", :rendering-engine "GECKO"}}))
