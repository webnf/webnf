(ns webnf.test-user-agent
  (:use clojure.test webnf.user-agent)
  (:require webnf.test-util))

(deftest test-user-agent
  (are [s ua] (= (parse-user-agent s) ua)

       "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.43 Safari/537.31"
       {:id 16912129, 
        :operating-system {:name "Linux", :device-type "Computer", 
                           :group "Linux", :id 258, 
                           :manufacturer "Other", :mobile false},
        :browser {:name "Chrome", :version "26.0.1410.43", :type "Browser", 
                  :id 3841, :manufacturer "Google Inc.", :rendering-engine "WEBKIT"}}

       "Mozilla/5.0 (X11; Linux x86_64; rv:17.0) Gecko/20100101 Firefox/17.0"
       {:id 16911626, 
        :operating-system {:name "Linux", :device-type "Computer", 
                           :group "Linux", :id 258, 
                           :manufacturer "Other", :mobile false},
        :browser {:name "Firefox", :version "17.0", 
                  :type "Browser", :id 3338, 
                  :manufacturer "Mozilla Foundation", :rendering-engine "GECKO"}}))
