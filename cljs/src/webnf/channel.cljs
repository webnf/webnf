(ns webnf.channel
  (:require
   [webnf.impl :refer [log]]
   [cljs.core.async.impl.dispatch :as dispatch]
   [cljs.core.async.impl.protocols :as async]
   [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn serve-read-waiters [waiters value]
  (doseq [handler waiters]
    (try (if (async/active? handler)
           (let [f (async/commit handler)]
             (dispatch/run #(f value)))
           (async/put! handler value (fn [])))
         (catch :default e
           (log "ERROR" "during serving of read handler" handler \newline
                "with value" value \newline e)))))

(deftype CallbackReadPort [read-callback ^:mutable read-waiters ^:mutable read-running]
  async/ReadPort
  (take! [p handler]
    (when (async/active? handler)
      (set! read-waiters (cons handler read-waiters))
      (when-not read-running
        (set! read-running true)
        (read-callback (fn [res]
                         (let [rw read-waiters]
                           (set! read-running false)
                           (set! read-waiters nil)
                           (serve-read-waiters rw res))))))
    nil))

(defn callback-read-port [f]
  (CallbackReadPort. f nil false))
