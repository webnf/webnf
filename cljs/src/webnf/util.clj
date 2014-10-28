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

(defmacro log-expr [& exprs]
  `(let [ret-exp# ~(last exprs)]
     (log "Result of" ~(pr-str (last exprs)) "=>"
          (cljs.core/pr-str ret-exp#) \newline
          ~@(mapcat
             (fn [e]
               `[~(pr-str e) "=>" (cljs.core/pr-str ~e) \newline])
             (butlast exprs)))
     ret-exp#))
