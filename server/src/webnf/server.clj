(ns webnf.server
  (:require 
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (org.eclipse.jetty.server Server)
   (org.eclipse.jetty.servlet ServletContextHandler ServletHolder)
   (org.eclipse.jetty.server ServerConnector)
   (org.eclipse.jetty.server.handler HandlerCollection HandlerList)
   (org.eclipse.jetty.util.thread ExecutorThreadPool)))

(set! *warn-on-reflection* true)

(defn make-handler [mapping servlet params]
  (doto (ServletContextHandler.)
    (.addServlet (doto (ServletHolder. servlet)
                   (.setInitParameters params)
                   (.setAsyncSupported true))
                 mapping)))

(defn add-vhost! [{:keys [jetty container handlers vhosts] :as ctx} 
                  id add-vhosts servlet-class params]
  (when (handlers id)
    (throw (ex-info (str "Handler " id " is already running" {:id id}))))
  (when-not (seq add-vhosts)
    (throw (ex-info (str "Need to specify at least 1 vhost for handler " id) {:id id})))
  (when-let [have (seq (set/intersection vhosts add-vhosts))]
    (throw (ex-info (str "Vhosts " (str/join ", " have) " are already mapped")
                    {:id id :add-vhosts add-vhosts :vhosts vhosts})))
  (let [handler (doto (make-handler "/" servlet-class params)
                  (.setVirtualHosts (into-array String add-vhosts)))]
    (.addHandler container handler)
    (-> ctx
        (update-in [:vhosts] into add-vhosts)
        (assoc-in  [:handlers id] handler))))

(defn remove-vhost! [{:keys [jetty container handlers vhosts] :as ctx}
                     id]
  (if-let [handler ^ServletContextHandler (handlers id)]
    (let  [remove-vhosts (set (.getVirtualHosts handler))]
      (.removeHandler container handler)
      (-> ctx
          (update-in [:vhosts] set/difference remove-vhosts)
          (update-in [:handlers] dissoc id)))
    (throw (ex-info (str "No handler with " id) {:id id}))))

(defn server [&{:keys [host port idle-timeout default-handler]
                :or {port 80}}]
  (let [container (HandlerCollection. true)
        server (Server.)
        connector (doto (ServerConnector. server)
                    (.setPort port)
                    (.setHost host))]
    (when idle-timeout (.setIdleTimeout connector idle-timeout))
    {:jetty (doto server
              (.setHandler (if default-handler
                             (doto (HandlerList.)
                               (.addHandler container)
                               (.addHandler default-handler))
                             container))
              (.addConnector connector)
              (.setStopTimeout 1000)
              (.setStopAtShutdown true))
     :container container
     :vhosts #{}
     :handlers {}}))

(defn start! [{:keys [^Server jetty] :as ctx}]
  (.start jetty)
  ctx)

(defn stop! [{:keys [^Server jetty] :as ctx}]
  (.stop jetty)
  (.join jetty)
  ctx)
