(ns webnf.js
  (:require [webnf.base :refer [scat]]
            #?(:cljs [goog.crypt.base64 :as b64])))

#?
(:clj
 (do (defmacro invoke-later
       "Use js/setTimeout to evaluate body at some later point.
  If the first element of body is a number, it is interpreted as
  milliseconds to wait before evaluating the rest"
       [& body]
       (let [[timeout body] (if (and (> (count body) 1)
                                     (number? (first body)))
                              [(first body) (rest body)]
                              [1 body])]
         `(js/setTimeout (fn ~(gensym "invoke-later") [] ~@body) ~timeout)))))

(let [escape-regex (js/RegExp. "[&<>\"']" "g")
      escape (fn [s]
               (case s
                 "&" "&amp;"
                 "<" "&lt;"
                 ">" "&gt;"g
                 "\"" "&quot;"
                 "'" "&#39;"))]
  (defn escape-html [s]
    (.replace escape-regex escape)))

(defn html-ctype? [content-type]
  (re-matches #"text/html\s*(;.*)?" (str content-type)))

(defn basic-auth-str [login password]
  (str "Basic " (b64/encodeString (str login ":" password) true)))

(def to-js
  "Makes a js object from a map"
  (comp (scat js-obj) (scat concat)))
