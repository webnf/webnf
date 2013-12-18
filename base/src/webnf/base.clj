(ns webnf.base
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]))

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
