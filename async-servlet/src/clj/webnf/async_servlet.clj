(ns webnf.async-servlet
  (:refer-clojure :exclude [flush chunk])
  (:import javax.servlet.AsyncContext)
  (:require [clojure.tools.logging :as log]
            [webnf.async-servlet.impl :as impl]
            [ring.util.servlet :as servlet]))

(defn status [^AsyncContext async-context status]
  (impl/set-status (.getResponse async-context) status))

(defn headers [^AsyncContext async-context headers]
  (impl/set-headers (.getResponse async-context) headers))

(defn flush [^AsyncContext async-context]
  (.flushBuffer (.getResponse async-context)))

(defn chunk [^AsyncContext async-context chunk]
  (impl/write-chunk (.getResponse async-context) chunk))

(defn complete [^AsyncContext async-context]
  (.complete async-context))

(def log-listener
  {:error (fn [evt]
            (log/error :error evt))
   :timeout (fn [evt]
              (log/debug :timeout evt))
   :complete (fn [evt]
               (log/debug :complete evt))})
