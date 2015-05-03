(ns webnf.promise
  (:require
   [webnf.channel :refer [serve-read-waiters]]
   [cljs.core.async.impl.dispatch :as dispatch]
   [cljs.core.async.impl.protocols :as async]
   [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn- wrap-error [e]
  (if (instance? js/Error e) e (js/Error e)))

(defprotocol IPromise
  (-deliver [p val])
  (-deliver-error [p e])
  (-on-deliver [p value-cb] [p value-cb error-cb]))

(deftype Promise [^:mutable value ^:mutable error ^:mutable waiters]
  Object
  (toString [_]
    (str "Promise:"
     (case value
       :webnf.promise/pending "Pending"
       :webnf.promise/error error
       (str "Delivered: " value))))
  async/ReadPort
  (take! [p handler]
    (when (async/active? handler)
      (case value
        :webnf.promise/pending (do (set! waiters (cons handler waiters))
                                   nil)
        :webnf.promise/error (do (async/commit handler)
                                 (volatile! error))
        (do (async/commit handler)
            (volatile! value)))))

  IDeref
  (-deref [p]
    (case value 
      :webnf.promise/pending (throw (ex-info "Can't deref a pending promise in cljs, sorry" {:webnf/promise p}))
      :webnf.promise/error (throw error)
      
      value))

  IPromise
  (-deliver-error [p e]
    (when-not (= value :webnf.promise/pending)
      (throw (ex-info "Can't deliver on a promise more than once" {:webnf/promise p :new-value val})))
    (set! value :webnf.promise/error)
    (set! error (wrap-error e))
    (serve-read-waiters waiters error)
    (set! waiters nil)
    nil)
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
