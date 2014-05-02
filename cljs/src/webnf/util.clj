(ns webnf.util)

(defmacro invoke-later
  "Use js/setTimeout to evaluate body at some later point.
  If the first element of body is a number, it is interpreted as
  milliseconds to wait before evaluating the rest"
  [& body]
  (let [[timeout body] (if (and (> (count body) 1)
                                (number? (first body)))
                         [(first body) (rest body)]
                         [1 body])]
    `(js/setTimeout (fn ~(gensym "invoke-later") [] ~@body) ~timeout)))

(defn to-str [n]
  (if (string? n)
    n (name n)))

(defn to-args [args]
  (if-not (sequential? args)
    (cons (to-str args) nil)
    (cons (to-str (first args))
          (rest args))))

;; ## jQuery helper macros
;;
;; As opposed to the jQuery helper _functions_ in webnf.util, these
;; are called with plain symbols, which are stringified by the macro.

(defmacro $->
  "Take obj and some calls like ->, but use $- to do the calls by
  javascript method name. Symbols in call position will be stringified
  for the method lookup. Useful to thread jQuery invokations and other
  kinds of interactions with external code without externs"
  [obj & calls]
  `(-> ~obj
       ~@(for [c calls
               :let [[m & args] (to-args c)]]
           `($a- ~m ~(when (seq args)
                       `(cljs.core/array ~@args))))))

(defmacro $>
  "Like $->, but call window.jQuery on init first. Used to start a
  jQuery call tree"
  [init & calls]
  `($-> ($a* (cljs.core/array ~@(to-args init)))
        ~@calls))

(defmacro log-expr [& exprs]
  `(let [ret-exp# ~(last exprs)]
     (log "Result of" ~(pr-str (last exprs)) "=>"
          (cljs.core/pr-str ret-exp#) \newline
          ~@(mapcat
             (fn [e]
               `[~(pr-str e) "=>" (cljs.core/pr-str ~e) \newline])
             (butlast exprs)))
     ret-exp#))
