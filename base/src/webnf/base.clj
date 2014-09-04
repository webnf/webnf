(ns webnf.base
  "Various primitive core operations that should be 'just there'"
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(defn make-autoloader
  "PRIVATE used by autoload macro"
  [replace-var var-ns var-name static]
  (fn [& args]
    (log/trace (str "Autoloading " var-ns " for " var-name))
    (require var-ns)
    (let [target-var (ns-resolve (find-ns var-ns) var-name)
          f (if static
              (deref target-var)
              target-var)]
      (alter-var-root replace-var (constantly f))
      (alter-meta! replace-var (constantly (meta target-var)))
      (apply f args))))

(defmacro autoload 
  "Pass a (unquoted) qualified symbol. Generates a var with same name,
  that will load the namespace on first invokation and put the foreign
  var into the generated var. If passed symbol has a ^:static
  metadata, it will put the contents of the foreign var into the
  generated var.

  (autoload foo/bar) -> (def bar #'foo/bar) ; on first call
  (autoload ^:static foo/bar) -> (def bar @#'foo/bar) ; on first call" [var-name]
  (let [mm (meta var-name)
        vn (with-meta (symbol (name var-name)) mm)
        vns (symbol (namespace var-name))]
    `(def ~vn (make-autoloader #'~vn '~vns '~vn ~(:static mm)))))

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
                      (map #(with-meta (symbol ns (name %)) mm)
                           (rest spec))))
                `(autoload ~spec)))))

(autoload clojure.pprint/pprint)

(defn hostname
  "Get the hostname of localhost"
  []
  (.. java.net.InetAddress getLocalHost getHostName))

(defn reset-logging-config!
  "Pass a path to a logback config (default logback.xml) to reset the
   logging configuration"
  ([] (reset-logging-config! "logback.xml"))
  ([logback-xml]
     (if-let [s (io/resource logback-xml)]
       (doto (ch.qos.logback.classic.joran.JoranConfigurator.)
         (.setContext (doto (org.slf4j.LoggerFactory/getILoggerFactory)
                        .reset))
         (.doConfigure s))
       (throw (ex-info (str "Not a resource " logback-xml) {:filename logback-xml})))))

(defn to-many
  "Ensure that seq can be called on a value. If value is not a coll
  and not nil, it is put into an empty collection"
  [v]
  (if (or (nil? v) (coll? v)) v (cons v nil)))

(defn pprint-str
  "Return value pretty-printed into a string"
  [o]
  (with-out-str (pprint o)))

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

(defmacro forcat
  "Concat the return value of a for expression"
  [bindings body]
  `(apply concat (for ~bindings ~body)))

(autoload clojure.reflect/reflect)

(defn pr-cls [^Class cls & {:as flags :keys [public private protected static fields methods declaring bases final abstract]}]
  (let [{:as want-flags? :keys [public private protected static fields methods declaring bases abstract]}
        (merge {:public true :private false :protected false :static true
                :fields true :methods true :bases true :declaring false
                :final true :abstract true}
               flags)
        rc (reflect cls :ancestors bases)
        want-member? (fn [{fl :flags dc :declaring-class pt :parameter-types :as mt}]
                       ;; (log/trace (:name mt) dc (keys mt) (:flags mt))
                       (cond #_#_ (and (not= (name dc) (.getName cls))
                                       (not bases))
                             false
                             (and (not pt) (not fields))
                             false
                             :else (every? want-flags?
                                           (if (or (:public fl)
                                                   (:private fl))
                                             (seq fl)
                                             (cons :protected fl)))))
        method-key (fn [{:keys [name return-type type declaring-class
                                parameter-types exception-types flags]}]
                                        ; fields > constructors > methods
                     [(cond (not parameter-types) 0
                            (not return-type) 1
                            :else 2)
                      (cond (:public flags) 0
                            (:private flags) 2
                            :else 1)
                      (if (:static flags) 1 0)
                      name])]
    (println
     (apply 
      str (concat (interpose " " (map name (:flags rc))) [" class "] [(.getName cls)]
                  (when (:bases rc)
                    (interpose " " (cons " extends" (map str (:bases rc)))))
                  [" {\n"]
                  (forcat [{:keys [name return-type type declaring-class
                                   parameter-types exception-types flags]}
                           (sort-by method-key (filter want-member? (:members rc)))]
                          (concat
                           ["    "]
                           (interleave (map clojure.core/name flags) (repeat " "))
                           [(or return-type type "new")
                            " " name]
                           (when parameter-types
                             (concat ["("] (interpose ", " parameter-types) [")"]
                                     (when (seq exception-types)
                                       (cons " throws " (interpose ", " exception-types)))))
                           [";"]
                           (when declaring [" // from " (str declaring-class)])
                           ["\n"]))
                  ["}\n"])))))
