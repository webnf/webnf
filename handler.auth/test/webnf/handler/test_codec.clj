(ns webnf.handler.test-codec
  (:require [clojure.test :as t :refer
             [deftest are is]]
            [webnf.handler.auth.codec :as sut :refer
             [decode-object encode-object]]))

(defrecord Foo [a b])

(deftest codec-roundtrip
  (are [same] (= same (decode-object (encode-object same)))
    {:ticket 1}
    (->Foo 1 2)))

