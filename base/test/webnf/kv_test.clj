(ns webnf.kv-test
  (:require [clojure.test :refer :all]
            [webnf.kv :refer :all]))

(deftest treduce-kv-tests
  (testing "Transient map reduce functions"
    (is (= (treduce-kv (fn [t k v]
                         (assoc-kv! t k (inc v)))
                       {:got 1} {:some 1 :more 2})
           {:got 1 :some 2 :more 3}))
    (is (= (treduce-kv (fn [t k v]
                         (assoc-kv! t k (inc v)))
                       {:got 1} [[:some 1] [:more 2]])
           {:got 1 :some 2 :more 3}))
    (is (= (treduce-kv (fn [t k v]
                         (assoc-kv! t k (inc v)))
                       [[:got 1]] [[:some 1] [:more 2]])
           [[:got 1] [:some 2] [:more 3]]))
    (is (= (map-juxt inc dec {0 2 1 3 2 4})
           {1 1 2 2 3 3}))
    (is (= (map-juxt inc dec [[0 2] [1 3] [2 4]])
           [[1 1] [2 2] [3 3]]))))

(deftest apply-kw-test
  (is (= (apply-kw (fn [a & {b :a}] (+ a b))
                   1 {:a 2})
         3)))

(deftest merge-deep-test
  (is (= (merge-deep
          {:a 1 :b 1 :c {:a 1 :b 1} :d {:a 1 :b 1} :e {:a 1 :b 1} :f 1}
          {:b 2 :d {:b 2} :e 2 :f {:b 2}})
         {:a 1 :b 2 :c {:a 1 :b 1} :d {:a 1 :b 2} :e 2 :f {:b 2}})))

(deftest assoc-when-test
  (let [res-str (with-out-str
                  (is (= (assoc-when pos? (do (println "Init")
                                              {})
                                     :keyA (do (print "Steps: 0")
                                               0)
                                     :keyB (do (print " 1")
                                               1)
                                     :keyC (do (print " 2")
                                               2)
                                     :keyD (do (println " end")
                                               -42))
                         {:keyB 1 :keyC 2})))]
    (is (= res-str "Init\nSteps: 0 1 2 end\n"))))
