(ns webnf.base
  (:require [clojure.java.io :as io]))

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
