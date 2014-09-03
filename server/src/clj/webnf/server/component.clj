(ns webnf.server.component
  (:require [com.stuartsierra.component :as cmp]
            [clojure.tools.logging :as log]
            [webnf.kv :refer [map-vals map-kv treduce-kv assoc-when*]]
            [webnf.server.util :refer [request-id-ceptor request-log-ceptor]]
            [clojure.set :as set]
            [clojure.string :as str])
  (:import (org.eclipse.jetty.server Server)
           (org.eclipse.jetty.servlet ServletContextHandler)
           (org.eclipse.jetty.server.handler HandlerCollection HandlerList)
           webnf.server.JettyHandlerWrapper))

(defrecord ServerComponent [^Server jetty container default-handler identify logging-queue hosts vhosts]
  cmp/Lifecycle
  (start [this]
    (if (.isRunning jetty)
      this
      (let [t0 (System/currentTimeMillis)
            handler (if default-handler
                      (doto (HandlerList.)
                        (.addHandler container)
                        (.addHandler default-handler))
                      container)]
        (.setHandler jetty (if (or identify logging-queue)
                             (JettyHandlerWrapper. handler
                                                   (when identify (request-id-ceptor identify))
                                                   (when logging-queue (request-log-ceptor logging-queue)))
                             handler))
        (log/info "Starting jetty ...")
        (.start jetty)
        (log/info "... waiting for listeners")
        (doseq [l (.getConnectors jetty)]
          (.getLocalPort l))
        (log/info "... jetty startup finished in" (- (System/currentTimeMillis) t0) "ms")
        (assoc this :hosts (cmp/start hosts)))))
  (stop [this]
    (if (.isRunning jetty)
      (do
        (.stop jetty)
        (.join jetty)
        (assoc this :hosts (cmp/stop hosts)))
      this)))

(defrecord HostComponent [^HandlerCollection container ^ServletContextHandler handler]
  cmp/Lifecycle
  (start [this]
    (.addHandler container handler)
    (.start handler)
    this)
  (stop [this]
    (.stop handler)
    (.removeHandler container handler)
    this))

(comment
  (defrecord PrComponent [id]
    cmp/Lifecycle
    (start [this]
      (println :start this)
      this)
    (stop [this] 
      (println :stop this)
      this))

  (defn subsystem-map [& {:as kvs}]
    (let [outer-rdeps (reduce 
                       (fn [deps cmp]
                         (reduce-kv (fn [res local dep]
                                      (if (or (kvs dep) (res dep))
                                        res
                                        (assoc res dep (gensym (name dep)))))
                                    deps (::cmp/dependencies (meta cmp))))
                       {} (vals kvs))
          outer-deps (map-kv (fn [assoc! dep dep-sym]
                               (assoc! dep-sym dep))
                             outer-rdeps)
          update-dep (fn [inner-deps local dep]
                       (assoc inner-deps local
                              (get outer-rdeps dep dep)))]
      (cmp/using (cmp/map->SystemMap (reduce-kv
                                      (fn [sub id cmp]
                                        (assert (not (sub id)))
                                        (assoc sub id
                                               (if (meta cmp)
                                                 (vary-meta cmp update-in [::cmp/dependencies]
                                                            (partial reduce-kv update-dep {}))
                                                 cmp)))
                                      outer-deps kvs))
                 outer-deps))))
