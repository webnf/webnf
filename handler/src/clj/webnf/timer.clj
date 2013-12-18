(ns webnf.timer
  (:import
   (java.util.concurrent TimeUnit ScheduledThreadPoolExecutor)))

(defonce executor (ScheduledThreadPoolExecutor. 0))

(defn recurring [period & {:keys [delay unit]}]
  (fn [thunk]
    (.scheduleWithFixedDelay executor thunk
                             (or delay period) period (or unit TimeUnit/MILLISECONDS))))

(defn recurring-fixed [period & {:keys [delay unit]}]
  (fn [thunk]
    (.scheduleAtFixedRate executor thunk
                          (or delay 0) period (or unit TimeUnit/MILLISECONDS))))

(defn in [delay & {:keys [unit]}]
  (fn [thunk]
    (.schedule executor thunk
               delay (or unit TimeUnit/MILLISECONDS))))

(defmacro schedule [op & body]
  `(op (fn [] ~@body)))
