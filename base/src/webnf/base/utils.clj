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
  "Return value pretty-printed into a string.
   Allows for clojure.pprint/*print-right-margin* to be passed as second argument"
  ([o] (with-out-str (pprint o)))
  ([o right-margin]
     (require 'clojure.pprint)
     (binding [clojure.pprint/*print-right-margin* right-margin]
       (pprint-str o))))

(defmacro forcat
  "Concat the return value of a for expression"
  [bindings body]
  `(apply concat (for ~bindings ~body)))

(defmacro static-case
  "Variant of case where keys are evaluated at compile-time
   WARNING: only use this for dispatch values with stable hashes,
     like edn literals, java Enums, ..."
  [val & cases]
  `(case ~val
     ~@(forcat [[field thunk] (partition 2 cases)]
               [(eval field) thunk])
     ~@(when (odd? (count cases))
         [(last cases)])))
