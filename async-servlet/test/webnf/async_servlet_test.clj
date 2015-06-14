(ns webnf.async-servlet-test
  (:use clojure.test)
  (:require
   [webnf.server :as srv]
   [webnf.server.component :as scmp]
   [com.stuartsierra.component :as cmp]))

(def test-port 32413)
(def server (agent (srv/server :host "localhost" :port test-port)))

(defn init-fn []
  (println "start"))

(defn destroy-fn []
  (println "stop"))

(defn handler-fn [req]
  (println "Handled")
  {:status 200 :body (pr-str (assoc req :seen :seen))})

(deftest roundtrip
  (send-off server cmp/start)
  (send-off server srv/add-handler
            :roundtrip (srv/servlet-handler
                        webnf.AsyncServlet {"webnf.handler.service"
                                            "webnf.async-servlet-test/handler-fn"
                                            "webnf.handler.init"
                                            "webnf.async-servlet-test/init-fn"
                                            "webnf.handler.destroy"
                                            "webnf.async-servlet-test/destroy-fn"}
                        "/")
            :vhosts ["localhost"]))
