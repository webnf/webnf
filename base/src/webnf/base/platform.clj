(ns webnf.base.platform
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [webnf.base.autoload :refer [autoload]]
            [webnf.base.util :refer [forcat]]))

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

(set! *warn-on-reflection* true)

(defn- ze-name ^String [^java.util.zip.ZipEntry ze]
  (.getName ze))

(defn zip-entries
  ([zip] (zip-entries zip (constantly true)))
  ([zip want-entry?]
   (with-open [zf (java.util.zip.ZipFile. (io/file zip))]
     (into [] (remove #_(do (println (ze-name %) "=>" (.endsWith (ze-name %) "/")))
                      #(.endsWith (ze-name %) "/")
                      (enumeration-seq
                       (.entries zf)))))))

(defn relativize [base path]
  (let [base (when base (.getCanonicalFile (io/file base)))
        path (io/file path)]
    (loop [acc ()
           ^java.io.File path' path]
      (when-not path'
        (throw (ex-info (str "Resource dir not contained in project root"
                             {:base base :path path})
                        {:base base :path path})))
      (if (= base path')
        (vec acc)
        (recur (cons (.getName path') acc)
               (.getParentFile path'))))))

(defn split-path [path]
  (loop [acc () ^java.io.File path (io/file path)]
    (if path
      (recur (cons (.getName path) acc)
             (.getParentFile path))
      (vec acc))))

(defn system-classpath-roots []
  (str/split (System/getProperty "java.class.path") #":"))

(defn classpath-resources
  ([] (classpath-resources (system-classpath-roots)))
  ([roots] (if (string? roots)
             (recur (str/split roots #":"))
             (forcat [r roots
                      :let [f (.getCanonicalFile (io/file r))
                            ze-meta {:classpath-entry f}]]
                     (cond
                       (.isDirectory f) (for [de (dir-entries f)]
                                          (with-meta (relativize f de)
                                            ze-meta))
                       (.isFile f)      (for [ze (zip-entries f)]
                                          (with-meta (split-path (str ze))
                                            ze-meta)))))))

(defn find-ambigous-resources
  ([] (find-ambigous-resources (classpath-resources)))
  ([pathes]
   (loop [[p0 & [p1 :as pn]] (sort pathes)
          duplicate-files {}]
     (let [cpe0 (:classpath-entry (meta p0))
           cpe1 (:classpath-entry (meta p1))]
       (cond
         (empty? pn) duplicate-files
         (and (= p0 p1)
              (not= cpe0 cpe1))
         (recur pn (update-in duplicate-files [p0] (fnil into #{}) [cpe0 cpe1]))
         :else (recur pn duplicate-files))))))
