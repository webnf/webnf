(ns webnf.middleware.user-agent
  (:require [webnf.user-agent :refer [parse-user-agent]]))

(defn wrap-user-agent [h]
  (fn [{:as req
        {ua "user-agent"} :headers}]
    (h
     (if ua
       (assoc req :user-agent (parse-user-agent ua))
       req))))
