(ns webnf.async-servlet-test
  (:use clojure.test)
  (:require
   [webnf.server :as srv]))

(def test-port 32413)
(def server (agent (srv/server :host "localhost" :port test-port)))

(defn init-fn [this]
  (println "start" this))

(defn destroy-fn [this]
  (println "stop" this))

(defn handler-fn [req]
  (println "Handled")
  {:status 200 :body (pr-str (assoc req :seen :seen))})

(deftest roundtrip
  (send-off server srv/start!)
  (send-off server srv/add-vhost! :roundtrip ["localhost"]
            webnf.AsyncServlet {"webnf.handler.service"
                                "webnf.async-servlet-test/handler-fn"
                                "webnf.handler.init"
                                "webnf.async-servlet-test/init-fn"
                                "webnf.handler.destroy"
                                "webnf.async-servlet-test/destroy-fn"}))
