(ns webnf.impl)

(defn log 
  "Log args directly to the browser console"
  [& args]
  (when-let [con js/console]
    (.apply (.-log con) con (to-array args))))

(defn log-pr 
  "Log pr-str of args to browser console"
  [& args]
  (apply log (map pr-str args)))

(when-not *print-fn* ;; TODO: make this configurable, somehow?
  (set! *print-fn* log-pr))
