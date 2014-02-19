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

(defmacro $->
  "Take obj and some calls like ->, but use $- to do the calls by
  javascript method name. Useful to thread jQuery invokations."
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
