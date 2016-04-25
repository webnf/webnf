(ns webnf.base.autoload
  (:require #?(:clj  [clojure.tools.logging :as log]
               :cljs [webnf.base.logging :as log])
            [webnf.base.cljc #?(:clj :refer :cljs :refer-macros) [defmacro*]]))
#?
(:clj
 (defn make-autoloader
   "PRIVATE used by autoload macro"
   [replace-var var-ns var-name static macro]
   (fn [& args]
     (log/trace (str "Autoloading " var-ns " for " var-name))
     (require var-ns)
     (let [target-var (ns-resolve (find-ns var-ns) var-name)
           f (if static
               (deref target-var)
               target-var)]
       (assert (= (boolean macro) (boolean (:macro (meta target-var))))
               (str replace-var " macro: " (boolean macro) "; " target-var " macro: " (not macro)))
       (alter-var-root replace-var (constantly f))
       (alter-meta! replace-var (constantly (meta target-var)))
       (apply f args)))))

(defmacro* autoload 
  "Pass a (unquoted) qualified symbol. Generates a var with same name,
  that will load the namespace on first invokation and put the foreign
  var into the generated var. If passed symbol has a ^:static
  metadata, it will put the contents of the foreign var into the
  generated var.

  (autoload foo/bar) -> (def bar #'foo/bar) ; on first call
  (autoload ^:static foo/bar) -> (def bar @#'foo/bar) ; on first call"
  [var-name]
  :clj
  (let [mm (meta var-name)
        vn (with-meta (symbol (name var-name)) mm)
        vns (symbol (namespace var-name))]
    `(def ~vn (make-autoloader #'~vn '~vns '~vn ~(:static mm) ~(:macro mm))))
  :cljs
  (let [mm (meta var-name)
        vn (with-meta (symbol (name var-name)) mm)]
    `(def ~vn ~var-name)))

(defmacro autoload-some
  "Autoload multiple vars like in import:
   
   (autoload
     bar/foo
     (bas goo hoo)
     ^:static (bat ioo joo))"
  [& specs]
  (cons 'do (for [spec specs]
              (if (coll? spec)
                `(autoload-some
                  ~@(let [ns (name (first spec))
                          mm (meta spec)]
                      (map #(with-meta (symbol ns (name %)) (merge mm (meta %)))
                           (rest spec))))
                `(autoload ~spec)))))
