(ns webnf.async-servlet-test
  (:use clojure.test)
  (:require
   [clojure.tools.logging :as log]
   [webnf.async-servlet.websocket :refer [->TextFrame]]
   [webnf.base :refer [pprint-str pprint]]
   [webnf.server :as srv]
   [webnf.server.component :as scmp]
   [com.stuartsierra.component :as cmp]
   [clj-http.client :as hc]
   [clojure.core.async :as async :refer [go go-loop <! >! close! put!]]))

(def test-port 32413)

(defn handler-fn [req]
  (if (get-in req [:headers "sec-websocket-key"])
    {:websocket true
     :body (fn [chan]
             (log/info "Websocket opened" chan)
             (def test-chan chan)
             (put! chan (->TextFrame "Server greetings!"))
             (go-loop []
               (if-let [msg (<! chan)]
                 (do (println "Got message:" (pprint-str msg))
                     (>! chan (->TextFrame (pr-str msg)))
                     (recur))
                 (close! chan))))}
    {:status 200 :body (pr-str {:seen true})}))

(use-fixtures
  :once
  (fn [run]
    (let [server (-> (srv/server :host "localhost" :http-port test-port)
                     (srv/add-handler
                      :roundtrip
                      {:handler (srv/servlet-handler
                                 webnf.AsyncServlet {"webnf.handler.service"
                                                     "webnf.async-servlet-test/handler-fn"
                                                     "webnf.handler.init"
                                                     "webnf.async-servlet-test/init-fn"
                                                     "webnf.handler.destroy"
                                                     "webnf.async-servlet-test/destroy-fn"}
                                 "/")
                       :vhosts ["localhost"]})
                     (cmp/start))]
      (run)
      (cmp/stop server))))

(deftest roundtrip
  (let [{:keys [status body]} (hc/get (str "http://localhost:" test-port "/"))]
    (is (= 200 status))
    (is (:seen (read-string body)))))
