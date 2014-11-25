(ns webnf.base.platform
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [webnf.base.autoload :refer [autoload]]
            [webnf.base.utils :refer [forcat]]))

(defn hostname
  "Get the hostname of localhost"
  []
  (.. java.net.InetAddress getLocalHost getHostName))

(defn local-ip
  "Get the ip of localhost"
  []
  (.. java.net.InetAddress getLocalHost getHostAddress))

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

(autoload clojure.reflect/reflect)
(defn pr-cls
  "Print class in java-like syntax"
  [^Class cls & {:as flags :keys [public private protected static fields methods declaring bases final abstract all]}]
  (let [{:as want-flags? :keys [public private protected static fields methods declaring bases abstract]}
        (merge {:public true :private false :protected false :static true
                :fields true :methods true :bases true :declaring false
                :final true :abstract true}
               flags)
        rc (reflect cls :ancestors bases)
        want-member? (fn [{fl :flags dc :declaring-class pt :parameter-types :as mt}]
                       ;; (log/trace (:name mt) dc (keys mt) (:flags mt))
                       (cond all true
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
                           (when declaring
                             [" // " (last (str/split (str declaring-class) #"\."))])
                           ["\n"]))
                  ["}\n"])))))
