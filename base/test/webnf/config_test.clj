(ns webnf.config-test
  (:require [webnf.config :as sut]
            [clojure.test :refer :all]))

(use-fixtures
  :each (fn [tests]
          (with-redefs
            [sut/configuration ::sut/unbound]
            (tests))))

(deftest toplevel-config
  (sut/set-configuration! {:a "a" :b "b"})
  (is (= "a" (sut/get-configuration [:a])))
  (is (thrown? clojure.lang.ExceptionInfo
               (sut/set-configuration! {:c "c"})))
  (is (= ::sut/unbound (sut/get-configuration [:c])))
  (sut/update-with-defaults! {:a "DEF a" :c "c"})
  (is (= "c" (sut/get-configuration [:c])))
  (is (= "a" (sut/get-configuration [:a]))))

(deftest sublevel-config
  (is (= ::sut/unbound (sut/get-configuration [:a])))
  (is (= ::sut/unbound (sut/get-configuration [:sub-a :A])))
  
  (sut/set-configuration! [:sub-a] {:a "a-a" :b "a-b"})
  (sut/set-configuration! [:sub-b] {:a "b-a" :b "b-b"})
  (is (= ::sut/unbound (sut/get-configuration [:a])))
  (is (= "a-a" (sut/get-configuration [:sub-a :a])))
  (is (= "b-a" (sut/get-configuration [:sub-b :a])))
  (is (thrown? clojure.lang.ExceptionInfo
               (sut/set-configuration! [:sub-a] {:c "c"})))
  (is (= ::sut/unbound (sut/get-configuration [:sub-a :c])))
  (sut/update-with-defaults! [:sub-a] {:a "DEF a" :c "c"})
  (is (= "c" (sut/get-configuration [:sub-a :c])))
  (is (= "a-a" (sut/get-configuration [:sub-a :a])))
  (is (= ::sut/unbound (sut/get-configuration [:sub-b :c])))
  (is (= "b-a" (sut/get-configuration [:sub-b :a]))))
