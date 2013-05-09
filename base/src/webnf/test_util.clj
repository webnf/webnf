(ns webnf.test-util
  (:require 
   [ring.mock.request :refer [request]]
   [clojure.data.codec.base64 :as base64]
   [clojure.test :refer [assert-expr report]]))

(defmethod assert-expr 'submap? [msg form]
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

(defn basic-auth-str [user pw]
  (str "Basic " (String. (base64/encode (.getBytes (str user \: pw) "UTF-8")))))

(defn basic-authenticated-request [method uri user pw]
  (assoc-in (request method uri)
            [:headers "authorization"]
            (basic-auth-str user pw)))

