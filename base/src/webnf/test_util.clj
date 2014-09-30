(ns webnf.test-util
  (:require 
   [clojure.test :refer [assert-expr report]]))
   [webnf.base :refer [autoload autoload-some forcat]]

(defmethod assert-expr 'submap?
  "(submap? super sub) form useable as a predicate in test-is.
  Checks for every entry in sub whether its value is equal."
  [msg form]
  (let [[_ supermap submap] form]
    `(let [supermap# ~supermap
           submap# ~submap]
       (doseq [[k# ev#] submap#]
         (let [gv# (supermap# k#)]
           (if (= ev# gv#)
             (report {:type :pass :message ~msg})
             (report {:type :fail :message ~msg
                      :expected (list '~'= (list k# supermap#) (list k# submap#))
                      :actual (list '~'not= gv# ev#)})))))))

;; ## Mock requests

(def ^:private b64-enc
  (delay (sun.misc.BASE64Encoder.)))

(defn basic-auth-str 
  "Construct a basic auth string for mock requests"
  [user pw]
  (str "Basic " (String. (.encode
                          ^sun.misc.BASE64Encoder @b64-enc
                          (.getBytes (str user \: pw) "UTF-8")))))

(defn basic-authenticate-request
  "Construct a basic-authenticated ring request"
  [req user pw]
  (assoc-in req [:headers "authorization"]
            (basic-auth-str user pw)))

