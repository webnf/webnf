(ns webnf.jq)

;; ### jQuery helper functions
;;
;; These functions just let you pass in strings to interact with
;; jQuery and other plain javascript method calls without externs.
;; The closure compiler will eliminate double string literals, so we
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
  [jq] (and jq (pos? (alength jq))))
