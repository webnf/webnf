(ns webnf.base.utils
  (:require [webnf.base.autoload :refer [autoload]]))

(defn to-many
  "Ensure that seq can be called on a value. If value is not a coll
  and not nil, it is put into an empty collection"
  [v]
  (if (or (nil? v) (coll? v)) v (cons v nil)))

(defmacro squelch 
  "Eval body with a handler for Exception that returns a default expression val.
  Logs exceptions on trace priority."
  [val & body]
  `(try ~@body (catch Exception e#
                 (let [val# ~val]
                   (log/trace e# "during execution of" 
                              (pprint-str '(try ~@body (catch Exception e ...)))
                              "\n used replacement value:" val#)
                   val#))))

(autoload clojure.pprint/pprint)
(defn pprint-str
  "Return value pretty-printed into a string"
  [o]
  (with-out-str (pprint o)))

(defmacro forcat
  "Concat the return value of a for expression"
  [bindings body]
  `(apply concat (for ~bindings ~body)))
