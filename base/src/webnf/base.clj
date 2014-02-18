(ns webnf.base
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(defn make-autoloader [replace-var var-ns var-name static]
  (fn [& args]
    (log/trace (str "Autoloading " var-ns))
    (require var-ns)
    (let [target-var (ns-resolve (find-ns var-ns) var-name)
          f (if static
              (deref target-var)
              target-var)]
      (alter-var-root replace-var (constantly f))
      (alter-meta! replace-var (constantly (meta target-var)))
      (apply f args))))

(defmacro autoload [var-name]
  (let [mm (meta var-name)
        vn (with-meta (symbol (name var-name)) mm)
        vns (symbol (namespace var-name))]
    `(def ~vn (make-autoloader #'~vn '~vns '~vn ~(:static mm)))))

(autoload clojure.pprint/pprint)

(defn hostname []
  (.. java.net.InetAddress getLocalHost getHostName))

(defn reset-logging-config!
  ([] (reset-logging-config! "logback.xml"))
  ([logback-xml]
     (if-let [s (io/resource logback-xml)]
       (doto (ch.qos.logback.classic.joran.JoranConfigurator.)
         (.setContext (doto (org.slf4j.LoggerFactory/getILoggerFactory)
                        .reset))
         (.doConfigure s))
       (throw (ex-info (str "Not a resource " logback-xml) {:filename logback-xml})))))

(defn to-many [v]
  (if (or (nil? v) (coll? v)) v (cons v nil)))

(defn pprint-str [o]
  (with-out-str (pprint o)))

(defmacro squelch [val & body]
  `(try ~@body (catch Exception e#
                 (let [val# ~val]
                   (log/trace e# "during execution of" 
                              (pprint-str '(try ~@body (catch Exception e ...)))
                              "\n used replacement value:" val#)
                   val#))))

(defmacro forcat [bindings body]
  `(apply concat (for ~bindings ~body)))

