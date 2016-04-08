(ns webnf.async
  #?(:clj (:require [clojure.tools.logging :as log]
                    [clojure.core.async :refer [<!!]]
                    [clojure.core.async.impl.dispatch :as dispatch]
                    [clojure.core.async.impl.protocols :refer
                     [ReadPort WritePort Channel
                      take! put! close! closed?
                      active? commit]])
     :cljs (:require [webnf.base.logging :as log]
                     [cljs.core.async.impl.dispatch :as dispatch]
                     [cljs.core.async.impl.protocols :refer
                      [ReadPort WritePort Channel
                       take! put! close! closed?
                       active? commit]])))

#?
(:clj
 (do
   (defn chan-seq
     "A lazy seq that gets its elements by taking from chan."
     [ch]
     (lazy-seq
      (when-let [x (<!! ch)]
        (cons x (chan-seq ch)))))))

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

(defn serve-read-waiters [waiters value]
  (doseq [handler waiters]
    (try (if (active? handler)
           (let [f (commit handler)]
             (dispatch/run #(f value)))
           (put! handler value (fn [])))
         (catch :default e
           (log/error "during serving of read handler" handler \newline
                      "with value" value \newline e)))))

(deftype CallbackReadPort
    #?(:clj  [read-callback
              ^:unsynchronized-mutable read-waiters
              ^:unsynchronized-mutable read-running]
       :cljs [read-callback
              ^:mutable read-waiters
              ^:mutable read-running])
    ReadPort
    (take! [p handler]
      (when (active? handler)
        #?(:clj
           ;; DRAGONS: untested locking code; this originated from the CLJS impl
           (when (locking p
                   (set! read-waiters (cons handler read-waiters))
                   (when-not read-running
                     (set! read-running true)
                     true))
             (read-callback (fn [res]
                              (let [rw read-waiters]
                                (locking p
                                  (set! read-running false)
                                  (set! read-waiters nil))
                                (serve-read-waiters rw res)))))
           :cljs
           (do (set! read-waiters (cons handler read-waiters))
               (when-not read-running
                 (set! read-running true)
                 (read-callback (fn [res]
                                  (let [rw read-waiters]
                                    (set! read-running false)
                                    (set! read-waiters nil)
                                    (serve-read-waiters rw res))))))))
      nil))

(defn callback-read-port [f]
  (CallbackReadPort. f nil false))
