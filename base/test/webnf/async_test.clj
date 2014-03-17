(ns webnf.async-test
  (:require [clojure.test :refer :all]
            [webnf.async :refer :all]
            [clojure.core.async :as async]))

(deftest chan-seq-test
  (is (= (chan-seq (async/to-chan [:a :b :c]))
         [:a :b :c])))
