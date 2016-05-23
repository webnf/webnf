(ns webnf.datomic.tx-listen
  (:import (java.util.concurrent BlockingQueue TimeUnit ThreadPoolExecutor))
  (:require
   [com.stuartsierra.component :as cmp]
   [datomic.api :as d]
   [clojure.tools.logging :as log]))

(defprotocol ITxListener
  (-active [l])
  (-deliver [l db])
  (-add-listener [l t lf]))

(defn do-deliver [listener basis-t db]
  (when (< basis-t (d/basis-t db))
    (try
      (listener db)
      (catch Exception e
        (log/error e "When delivering to listener" listener)))))

(deftype TxListener [db-uri connection report-queue
                     ^:unsynchronized-mutable latest-db
                     ^:unsynchronized-mutable listeners]
  ITxListener
  (-active [this]
    (locking this (not (nil? latest-db))))
  (-deliver [this db]
    (assert connection "Not started")
    (let [new-basis-t (d/basis-t db)]
      (locking this
        (reduce-kv
         (fn [r listener basis-t]
           (if (< basis-t new-basis-t)
             (do (do-deliver listener basis-t db)
                 r)
             (assoc r listener basis-t)))
         {} listeners))))
  (-add-listener [this basis-t listener]
    (assert connection "Not started")
    (if (< basis-t (d/basis-t (locking this latest-db)))
      (do-deliver listener basis-t latest-db)
      (locking this
        (set! (.-listeners this) (assoc listeners listener basis-t))))
    nil)
  cmp/Lifecycle
  (start [this]
    (if connection
      this
      (let [connection (d/connect db-uri)]
        (future-call
         (fn listen []
           (if (-active this)
             (let [{db :db-after} (.poll ^BlockingQueue report-queue 10 TimeUnit/SECONDS)]
               (when db (-deliver this db))
               (recur))
             (log/info "Shutting down"))))
        (TxListener. db-uri connection
                     (d/tx-report-queue connection)
                     (d/db connection)
                     {}))))
  (stop [this]
    (if connection
      (do
        (d/remove-tx-report-queue connection)
        (locking this (set! (.-listeners this) nil))
        (TxListener. db-uri nil nil nil nil))
      this)))

(defn listener [db-uri]
  (->TxListener db-uri nil nil nil nil))
(defn add-listener! [listener basis-t listener-f]
  (-add-listener listener basis-t listener-f))
