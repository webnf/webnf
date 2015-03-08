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
  [cls & {:as flags :keys [public private protected static fields methods declaring bases final abstract all]}]
  (let [{:as want-flags? :keys [public private protected static fields methods declaring bases abstract]}
        (merge {:public true :private false :protected false :static true
                :fields true :methods true :bases true :declaring false
                :final true :abstract true}
               flags)
        rc (reflect cls
                    :ancestors bases
                    :reflector (clojure.reflect.JavaReflector.
                                (or (and (class? cls) (.getClassLoader ^Class cls))
                                    (.getContextClassLoader (Thread/currentThread)))))
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

(defn dir-entries
  ([dir] (dir-entries dir (constantly true)))
  ([dir want-file?]
   (forcat [e (.listFiles (io/file dir))]
           (cond (.isDirectory e) (dir-entries e want-file?)
                 (want-file? e)   [e]))))

(defn zip-entries
  ([zip] (zip-entries zip (constantly true)))
  ([zip want-entry?]
   (with-open [zf (java.util.zip.ZipFile. (io/file zip))]
     (filterv want-entry?
              (enumeration-seq
               (.entries zf))))))

#_(defn relativize [base-path target-path]
    (.getPath
     (.relativize (.toURI (io/file base-path))
                  (.toURI (io/file target-path)))))

(defn relativize [base path]
  (loop [acc ()
         ^java.io.File path' (io/file path)]
    (if (= base path')
      (vec acc)
      (recur (cons (.getName path') acc)
             (.getParentFile path')))))

(defn classpath-resources
  ([] (classpath-resources (System/getProperty "java.class.path")))
  ([roots] (if (string? roots)
             (recur (str/split roots #":"))
             (forcat [r roots
                      :let [f (io/file r)]]
                     (cond
                       (.isDirectory f) (for [de (dir-entries f)]
                                          (with-meta (relativize f de)
                                            {:classpath-entry f}))
                       (.isFile f)      (for [ze (zip-entries f)]
                                          (with-meta (relativize nil (str ze))
                                            {:classpath-entry f})))))))

(defn find-ambigous-resources
  ([] (find-ambigous-resources (classpath-resources)))
  ([pathes]
   (loop [[p0 & [p1 :as pn]] (sort pathes)
          duplicate-files {}
          duplicate-folders {}]
     (let [cpe0 (:classpath-entry (meta p0))
           cpe1 (:classpath-entry (meta p1))]
       (cond
         (empty? pn) (merge duplicate-files duplicate-folders)
         (= p0 p1)
         (recur pn
                (update-in duplicate-files [p0] (fnil into #{}) [cpe0 cpe1])
                duplicate-folders)
         (and
          (< 1 (count p0))
          (< 1 (count p1))
          (not= cpe0 cpe1)
          (= (butlast p0)
             (butlast p1)))
         (recur pn
                duplicate-files
                (-> duplicate-folders
                    (update-in [(butlast p0)] (fnil into #{}) [cpe0 cpe1])))
         :else (recur pn duplicate-files duplicate-folders))))))
