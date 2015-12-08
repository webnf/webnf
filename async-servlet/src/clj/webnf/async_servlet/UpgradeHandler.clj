(ns webnf.async-servlet.UpgradeHandler
  (:require [clojure.tools.logging :as log])
  (:gen-class :implements [javax.servlet.http.HttpUpgradeHandler]
              :state state
              :init construct))

(def ^:dynamic *init*)

(defn -construct []
  (assert (bound? #'*init*) "Please bind *init* with an fn of signature
    (fn on-init [conn]
      (fn on-close []))")
  (log/trace "Creating websocket upgrade handler" *init*)
  [[] (volatile! *init*)])
(defn -init [this conn]
  (log/trace "Initializing websocket upgrade handler" this conn (.-state this))
  (vswap! (.-state this) #(%1 %2) conn))
(defn -destroy [this]
  (log/trace "Destroying websocket upgrade handler" this (.-state this))
  (vswap! (.-state this) #(do (%1) nil)))

(defn create [on-init]
  (binding [*init* on-init]
    (webnf.async_servlet.UpgradeHandler.)))
