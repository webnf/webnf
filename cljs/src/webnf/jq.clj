(ns webnf.jq)

;; ## jQuery helper macros
;;
;; As opposed to the jQuery helper _functions_ in webnf.util, these
;; are called with plain symbols, which are stringified by the macro.

(defn to-str [n]
  (if (string? n)
    n (name n)))

(defn to-args [args]
  (if-not (sequential? args)
    (cons (to-str args) nil)
    (cons (to-str (first args))
          (rest args))))

(defmacro ->*
  "Take obj and some calls like ->, but use -a* to do the calls by
  javascript method name. Symbols in call position will be stringified
  for the method lookup. Useful to thread jQuery invokations and other
  kinds of interactions with external code without externs"
  [obj & calls]
  `(-> ~obj
       ~@(for [c calls
               :let [[m & args] (to-args c)]]
           `(-a* ~m ~(when (seq args)
                       `(cljs.core/array ~@args))))))

(defmacro $>*
  "Like ->*, but call window.jQuery on init first. Used to start a
  jQuery call tree"
  [init & calls]
  `(->* ($a* (cljs.core/array ~@(to-args init)))
        ~@calls))
