(ns webnf.test-dataflow
  (:require
   [clojure.test :refer :all]
   [webnf.dataflow :refer :all]))

(deftest basic-flow
  (are [data df result]
      (= result (flow df data))
    "1" (coerce Integer) (int 1)))

(def map-flow
  (mapping :a (coerce Long)
           :b (optional (pred any?) :default :def)
           :c (optional (pred pos?))))

(deftest mapping-flow
  (is (= {:a 1 :b :def}
         (flow map-flow {:a "1"})))
  (are [data df]
      (error? (flow df data))
    {} map-flow
    {:a "non-int"} map-flow))

(deftest error-handling
  (are [data df msg-re]
      (thrown-with-msg?
       clojure.lang.ExceptionInfo
       msg-re (assert-value! df data))
    {:a "non-int" :c 1} map-flow #"Fields \[:a\] didn't validate"
    {:a "non-int" :c -1} map-flow #"Fields \[:c, :a\] didn't validate"))

(deftest wrapped-error
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Fields \[:a\] didn't validate"
       (assert-value!
        (protect-flow (flow map-flow {:a "non-int"}))))))
