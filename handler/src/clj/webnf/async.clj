(ns webnf.async
  (:require
   [webnf.timer :refer [schedule recurring]]
   [clojure.core.async :as async :refer [go <! <!! >! >!! close!]]
   [clojure.tools.logging :as log]))

(defn init [& {:keys [timeout buf-size block] :as default-config}]
  (agent {:default-config default-config
          :channels {}}))

(defn bump-channel [chan-map channel timeout]
  ())
