(ns webnf.util-test
  (:require #?@(:clj  [[clojure.test :as t]
                       [webnf.base.util :as u]]

                :cljs [[cljs.test :as t :include-macros true]
                       [webnf.base.util :as u :include-macros true]]))
  #?(:cljs (:require-macros webnf.util-test)))

(t/deftest test-squelsh
  (t/is (= ::value (u/squelch ::default ::value)))
  (t/is (= ::default (u/squelch ::default (throw (ex-info "error" {})))))
  (t/is
   #? (:clj  (thrown? Throwable (u/squelch ::default
                                           (throw (Throwable. "non-exception throwable"))))
       :cljs (= [::thrown "non-error throwable"]
                (try (u/squelch ::default
                                (throw "non-error throwable"))
                     (catch :default e
                       [::thrown e]))))))

(t/deftest test-forcat
  (t/is (= [2 10 3 20]
           (u/forcat [x [1 2]]
                     [(inc x) (* x 10)])))
  (t/is (thrown? #?(:clj Exception :cljs js/Error) (doall (u/forcat [x [1 2]] x)))))

#?(:clj
   (do (def ONE 1)
       (def TWO 2)))

(defn match-static [val]
  (u/static-case
   val
   ONE :match-one
   TWO :match-two
   :match-default))

(t/deftest test-static-case
  (t/is (= :match-one (match-static 1)))
  (t/is (= :match-two (match-static 2)))
  (t/is (= :match-default (match-static nil))))

(t/deftest test-href-path
  (t/is (= [] (u/href->path "/")))
  (t/is (= ["foo"] (u/href->path "/foo")))
  (t/is (= ["foo"] (u/href->path "/foo/")))
  (t/is (= ["foo" "bar"] (u/href->path "/foo/bar")))
  (t/is (= ["foo" "bar"] (u/href->path "/foo/bar/")))
  (t/is (thrown? #?(:clj Exception :cljs js/Error)
                 (u/href->path "")))
  (t/is (thrown? #?(:clj Exception :cljs js/Error)
                 (u/href->path "foo")))
  (t/is (thrown? #?(:clj Exception :cljs js/Error)
                 (u/href->path "foo/path"))))

(t/deftest test-str-quote
  (t/is (= "\"fo'o\\\"ba'r\"" (u/str-quote "fo'o\"ba'r")))
  (t/is (= "'fo\\'o\"ba\\'r'" (u/str-quote "fo'o\"ba'r" \')))
  (t/is (= "$f$$o'o\"b$$a'r$" (u/str-quote "f$o'o\"b$a'r" \$ \$))))
