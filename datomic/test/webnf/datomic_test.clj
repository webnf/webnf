(ns webnf.datomic-test
  (:require [clojure.test :refer :all]
            [webnf.datomic :refer :all]
            [webnf.base :refer :all]
            [clojure.core.unify :refer [unifier unify]]
            [clojure.tools.logging :as log]
            [datomic.api :as d]
            webnf.test-util))

;; # Test DB Schema

;; This is an example of how to use the schema helpers

;; functions defined with defn-db are usable as regular vars in the peer
(defn-db ::get-balance
  "Gets the current balance of an account"
  [db id]
  (if-let [e (d/entity db id)]
    (::balance e 0)
    (throw (ex-info (str "Entity doesn't exist " id) {:id id}))))

(def test-schema
  (concat
   ;; the def'ed var carries the db function tx in metadata
   (dbfn-tx #'get-balance #'compose-tx)
   ;; regular schema
   [(field "Account balance" ::balance :long)
    (field "Alternate identity" ::id :uuid :unique :identity)
    (function ::transfer "Transfers an amount between two accounts"
              {:db-requires [[::get-balance]]}
              [db amount from-id to-id]
              (let [fb (get-balance db from-id)
                    _ (when (> amount fb)
                        (throw (ex-info "Transfer not possible"
                                        {:sounce from-id :balance fb :amount amount})))]
                [[:db/add from-id ::balance (- fb amount)]
                 [:db/add to-id ::balance (+ (get-balance db to-id) amount)]]))]))

(def test-data
  [{:db/id (d/tempid :db.part/user)
    :db/ident ::acc-1
    ::balance 100}
   {:db/id (d/tempid :db.part/user)
    :db/ident ::acc-2
    ::balance 100}
   {:db/id (d/tempid :db.part/user)
    :db/ident ::acc-3
    ::balance 100}])

;; # Nitty gritty test setup stuff

(def ^:dynamic *test-run*)
(def ^:dynamic *conn*)

(use-fixtures
  ;; This fixture gives each test run a unique identity
  :once (fn [run]
          (let [id (d/squuid)]
            (binding [*test-run* {:run-id id}]
              (log/trace "Starting test run" id @*report-counters*)
              (let [res (run)]
                (log/trace "Test" id "finished with" res)
                res)))))
(use-fixtures
  :each
  ;; This fixture gives each test in a run a unique identity,
  ;; and creates a fresh database for it.
  ;; If the test fails, the database is kept for further inspection
  (fn [run]
    (let [id (d/squuid)
          test-name (str (:run-id *test-run*) "/" id)
          uri (str "datomic:mem://test-" test-name)
          created (d/create-database uri)
          fail-0 (:fail @*report-counters* 0)]
      (assert created)
      (log/trace "Created database" uri \newline "for test" test-name)
      (binding [*test-run* (assoc *test-run*
                             :test-id id)
                *conn* (connect uri)]
        (let [res (testing (str "with test database " uri)
                    (run))
              fail (- (:fail @*report-counters* 0) fail-0)]
          (log/trace "Tests finished" @*report-counters*)
          (if (zero? fail)
            (do (log/trace "Tests finished, deleting db" uri)
                (d/delete-database uri)
                res)
            (do (log/info "Tests failed, keeping db" uri)
                res))))))
  ;; This fixture inserts test data into the fresh database
  (fn [run]
    (let [t1 (d/transact *conn* test-schema)
          t2 (d/transact *conn* test-data)]
      @t1
      @t2
      (run))))

;; # Test routines

;; ## Utils

(defn gb [acct]
  (get-balance (d/db *conn*) acct))

(defn is-balance [balance a1 a2]
  (let [db (d/db *conn*)
        b1 (get-balance db a1)
        b2 (get-balance db a2)]
    (is (= balance (- b1 b2))
        (str "Required balance " balance " not met by "
             (- b1 b2) " = " a1 ":" b1 " - " a2 ":" b2))))

;; ## Tests

(deftest smoke-test
  (is (= 100 (gb ::acc-1)))
  (is (= 100 (gb ::acc-2)))
  (is (= 100 (gb ::acc-3))))

(deftest db-fn-example
  (is-balance 0 ::acc-1 ::acc-2)
  (let [db (d/db *conn*)]
    (is (= 
         ((:db/fn (d/entity db ::transfer))
          ;; get a db fn, that has not been bound to a var
          db 5 ::acc-1 ::acc-2)
         [[:db/add ::acc-1 ::balance 95]
          [:db/add ::acc-2 ::balance 105]])))
  @(d/transact *conn* [[::transfer 5 ::acc-2 ::acc-1]])
  (is-balance 10 ::acc-1 ::acc-2))

(deftest test-transaction-composer
  (let [db (d/db *conn*)
        id (d/squuid)]
    (is (match? (compose-tx db [[[:db/add ::acc-3 ::id id]]
                                [[::transfer 7 ::acc-1 [::id id]]]])
                (list [:db/add '?id3 :webnf.datomic-test/id id]
                      [:db/add '?id1 :webnf.datomic-test/balance 93]
                      [:db/add '?id3 :webnf.datomic-test/balance 107])))
    @(d/transact
      *conn* [[:webnf.datomic/compose-tx
               [[[:db/add ::acc-3 ::id id]]
                [[::transfer 7 ::acc-1 [::id id]]]
                [[::transfer 13 ::acc-2 ::acc-1]]]]]))
  (is-balance -1 ::acc-1 ::acc-3)
  (is-balance 19 ::acc-1 ::acc-2)
  (is-balance 20 ::acc-3 ::acc-2))
