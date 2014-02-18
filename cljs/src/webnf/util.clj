(ns webnf.util)

(defmacro invoke-later [& body]
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

(defmacro $-> [obj & calls]
  `(-> ~obj
       ~@(for [c calls
               :let [[m & args] (to-args c)]]
           `($a- ~m ~(when (seq args)
                       `(cljs.core/array ~@args))))))

(defmacro $> [init & calls]
  `($-> ($a* (cljs.core/array ~@(to-args init)))
        ~@calls))
