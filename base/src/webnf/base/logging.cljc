(ns webnf.base.logging
  #?(:clj (:require [webnf.base.cljc :refer [defmacro*]])
     :cljs (:require-macros [webnf.base.logging :refer [deflogfn]])))

(def
  ^{:doc "Root logging level. Set at compile time to omit logging statements"}
  ^:dynamic *root-log-level* :trace)

(def log-levels
       {:trace #{:trace :debug :info :warn :error}
        :debug #{:debug :info :warn :error}
        :info  #{:info :warn :error}
        :warn  #{:warn :error}
        :error #{:error}})

#?
(:cljs
 (do (defn log 
       "Log args directly to the browser console"
       [& args]
       (when-let [con js/console]
         (.apply (.-log con) con (to-array args))))

     (defn log-pr 
       "Log pr-str of args to browser console"
       [& args]
       (apply log (map pr-str args)))

     (when-not *print-fn* ;; TODO: make this configurable, somehow?
       (set! *print-fn* log-pr)))
 :clj
 (do (defmacro* log-expr [& exprs]
       :cljs `(let [ret-exp# ~(last exprs)]
                (log "Result of" ~(pr-str (last exprs)) "=>"
                     (cljs.core/pr-str ret-exp#) \newline
                     ~@(mapcat
                        (fn [e]
                          `[~(pr-str e) "=>" (cljs.core/pr-str ~e) \newline])
                        (butlast exprs)))
                ret-exp#))
     (defmacro do-log [level args]
       `(when-let [con# (and (contains? (get log-levels *root-log-level*) ~level)
                             js/console)]
          (.apply (. con# ~(symbol (str "-" (name level))))
                  con# (to-array ~args))))
     (defmacro* deflogfn [level]
       :cljs
       `(defn ~(symbol (name level)) [& args#]
          (do-log ~level args#))
       :clj
       `(defmacro ~(symbol (name level)) [& args#]
          (when (contains? (get log-levels *root-log-level*) ~level)
            (list `do-log ~level (vec args#)))))
     (defmacro spy* [& exprs]
         `(let [ret-exp# ~(last exprs)]
            (trace "Result of" '~(last exprs) "=>"
                   ret-exp# \newline
                   ~@(mapcat
                      (fn [e]
                        `['~e "=>" ~e \newline])
                      (butlast exprs)))
            ret-exp#))
     (defmacro spy [& exprs]
       `(let [ret-exp# ~(last exprs)]
          (trace "Result of" ~(pr-str (last exprs)) "=>"
                 (cljs.core/pr-str ret-exp#) \newline
                 ~@(mapcat
                    (fn [e]
                      `[~(pr-str e) "=>" (cljs.core/pr-str ~e) \newline])
                    (butlast exprs)))
          ret-exp#))))

(deflogfn :trace)
(deflogfn :debug)
(deflogfn :info)
(deflogfn :warn)
(deflogfn :error)
