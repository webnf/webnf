(ns webnf.promise
  (:require
   [webnf.util :refer [log]]
   [webnf.channel :refer [serve-read-waiters]]
   [cljs.core.async.impl.dispatch :as dispatch]
   [cljs.core.async.impl.protocols :as async]
   [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defprotocol IPromise
  (-deliver [p val])
  (-deliver-error [p e]))

(deftype Promise [^:mutable value ^:mutable error ^:mutable waiters]
  async/ReadPort
  (take! [p handler]
    (when (async/active? handler)
      (if (= :webnf.promise/pending value)
        (do (set! waiters (cons handler waiters))
            nil)
        (do (async/commit handler)
            p))))

  IDeref
  (-deref [p]
    (condp = value 
      :webnf.promise/pending
      (throw (ex-info "Can't deref a pending promise in cljs, sorry" {:webnf/promise p}))
      :webnf.promise/error
      error
      
      value))

  IPromise
  (-deliver-error [p e]
    (when-not (= value :webnf.promise/pending)
      (throw (ex-info "Can't deliver on a promise more than once" {:webnf/promise p :new-value val})))
    (set! value :webnf.promise/error)
    (set! error e))
  (-deliver [p val]
    (when-not (= value :webnf.promise/pending)
      (throw (ex-info "Can't deliver on a promise more than once" {:webnf/promise p :new-value val})))
    (set! value val)
    (serve-read-waiters waiters val)
    (set! waiters nil)
    nil))

(defn deliver
  "Deliver on a promise akin to clojure.core/deliver"
  [p val]
  (-deliver p val))

(defn deliver-error
  "Deliver an error to a promise"
  [p e]
  (-deliver-error p e))

(defn promise
  "Create a promise, which can be used much like a read channel,
  except that reads always return immediately, once the promise has
  been delivered on."
  ([]
     (Promise. :webnf.promise/pending nil nil))
  ([source-ch]
     (let [p (promise)]
       (go (try (deliver p (<! source-ch))
                (catch js/Error e
                  (deliver-error p e))))
       p)))
