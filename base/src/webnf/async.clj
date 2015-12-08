(ns webnf.async
  (:require
   [clojure.core.async :refer [<!!]]
   [clojure.core.async.impl.protocols :refer
    [ReadPort WritePort Channel
     take! put! close! closed?]]))

(defn chan-seq
  "A lazy seq that gets its elements by taking from chan."
  [ch]
  (lazy-seq
   (when-let [x (<!! ch)]
     (cons x (chan-seq ch)))))

(defn rw-chan [read-port write-port]
  (reify
    Channel
    (close! [_]
      (close! write-port)
      (close! read-port))
    (closed? [_]
      (or (closed? write-port)
          (closed? read-port)))
    ReadPort
    (take! [_ fn-handler]
      (take! read-port fn-handler))
    WritePort
    (put! [_ val fn-handler]
      (put! write-port val fn-handler))))
