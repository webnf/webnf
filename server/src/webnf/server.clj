(ns webnf.server
  (:require 
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [ring.util.servlet :as servlet]
;   [webnf.async-servlet.impl :as asi]
   )
  (:import
   (org.eclipse.jetty.server Server Request)
   (org.eclipse.jetty.servlet ServletContextHandler ServletHolder)
   (org.eclipse.jetty.server ServerConnector)
   (org.eclipse.jetty.server.handler HandlerCollection HandlerList)
   (org.eclipse.jetty.util.thread ExecutorThreadPool)
   (org.eclipse.jetty.server.handler AbstractHandler)))

(set! *warn-on-reflection* true)

(comment
  (defn- proxy-handler
    "Returns an Jetty Handler implementation for the given Ring handler."
    [handler]
    (proxy [AbstractHandler] []
      (handle [_ ^Request base-request request response]
        (asi/handle-servlet-request handler request response)
        (.setHandled base-request true))))
  (defn make-ring-handler [handler mapping]
    (doto (ServletContextHandler.)
      (.addServlet (doto (ServletHolder.)
                     (.setServletHandler (proxy-handler handler))
                     (.setAsyncSupported true))
                   mapping))))

(defn make-servlet-handler [servlet-class params mapping]
  (doto (ServletContextHandler.)
    (.addServlet (doto (ServletHolder. servlet-class)
                   (.setInitParameters params)
                   (.setAsyncSupported true))
                 mapping)
    (.setClassLoader (.getClassLoader servlet-class))))

(defn add-vhost! 
  ([{:keys [jetty container handlers vhosts] :as ctx} 
    id add-vhosts servlet-context-handler]
     (when (handlers id)
       (throw (ex-info (str "Handler " id " is already running" {:id id}))))
     (when-let [have (seq (set/intersection vhosts (set add-vhosts)))]
       (throw (ex-info (str "Vhosts " (str/join ", " have) " are already mapped")
                       {:id id :add-vhosts add-vhosts :vhosts vhosts})))
     (.addHandler container (doto servlet-context-handler
                              (.setVirtualHosts (into-array String add-vhosts))))
     (-> ctx
         (update-in [:vhosts] into add-vhosts)
         (assoc-in  [:handlers id] servlet-context-handler))))

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
