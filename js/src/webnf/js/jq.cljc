(ns webnf.js.jq)

#?
(:clj
 (do
   ;; ## jQuery helper macros
   ;;
   ;; As opposed to the jQuery helper _functions_ in webnf.dom.jq, these
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
           ~@calls)))
 :cljs
 (do
   ;; ### jQuery helper functions
   ;;
   ;; These functions just let you pass in strings to interact with
   ;; jQuery and other plain javascript method calls without externs.
   ;; The closure compiler will intern string literals, so we
   ;; can get away with this.

   (defn $a*
     "Apply window.jQuery to a js array of args"
     [arrgs]
     (.apply (aget js/window "jQuery")
             nil
             arrgs))

   (defn $* 
     "Apply window.jQuery to args"
     [& args]
     ($a* (to-array args)))

   (defn -a*
     "Call string method on obj with array of args"
     [obj meth arrgs]
     (and obj
          (.apply (aget obj meth) obj arrgs)))

   (defn -* 
     "Call string method on obj with args"
     [obj meth & args]
     (-a* obj meth (to-array args)))

   (defn $$ 
     "Call window.jQuery.<method> with args"
     [meth & args]
     (-a* (aget js/window "jQuery") meth (to-array args)))

   (defn $
     "Call window.jQuery(obj).<method> with args"
     ([obj] ((aget js/window "jQuery") obj))
     ([obj meth & args]
      (-a* ($ obj) meth (to-array args))))

   (defn $?
     "jQuery predicate: array with length > 0"
     [jq] (and jq (pos? (alength jq))))))

