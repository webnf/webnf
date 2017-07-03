(ns webnf.handler.test-auth
  (:require [webnf.handler.auth :as sut :refer
             [date< date> grant-signature-valid? validate-grant
              ->TicketRequest sign-grant auth-server auth-client
              server-handler wrap-client wrap-protect
              decode-ticket encode-ticket parse-header]]
            [clojure.spec.alpha :as s]
            [clojure.test :as t :refer
             [deftest testing is are assert-expr do-report]]
            [ring.mock.request :as mock]
            [ring.util.response :as rur]
            [ring.util.codec :as rcodec])
  (:import (java.util Date)))

(deftest date-comparison
  (is (date< #inst "2015" #inst "2016"))
  (is (date> #inst "2016" #inst "2015"))
  (is (not (date< #inst "2015" #inst "2015")))
  (is (not (date> #inst "2015" #inst "2015"))))

(deftest ticket-validation
  (let [ti {:public-key "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE4Wt9keq71vtffMK2KtUfTBb3dvvg7dvGN3dXiLlS4zFTAbL+nap55aHhmpVKTF2cmEotvkt3dTRQ+4wcKfaMqQ==",
            :private-key "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDUKuVmLhSVupWDzT208DFnW3n9ebx1Hl/2vgk3tnjMRg=="}
        aid #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"
        r (->TicketRequest aid
                           #inst "2016-01-01"
                           #inst "2016-01-02")
        gra (sign-grant ti #uuid "54100d61-7e85-4266-b0eb-e52ffcb79055" {:name "U Ser"}
                        #inst "2016-01-01T02"
                        #inst "2016-01-02"
                        r)]
    (is (grant-signature-valid? ti gra))
    (is (s/valid? ::sut/granted-ticket
                  (validate-grant ti #inst "2016-01-01T03" aid gra)))
    (is (= :expired
           (:failure-reason
            (validate-grant ti #inst "2016-01-02T03" aid gra))))
    (is (= :not-yet-valid
           (:failure-reason
            (validate-grant ti #inst "2015-12-30" aid gra))))
    (is (= :app-mismatch
           (:failure-reason
            (validate-grant ti #inst "2016-01-01T03"
                            #uuid "54100d61-7e85-4266-b0eb-e52ffcb79056"
                            gra))))
    (is (= :signature-mismatch
           (:failure-reason
            (validate-grant ti #inst "2016-01-01T03" aid (assoc-in gra [:user-info :extra] :super)))))))

(deftest grant-client-server
  (let [ti {:public-key "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE4Wt9keq71vtffMK2KtUfTBb3dvvg7dvGN3dXiLlS4zFTAbL+nap55aHhmpVKTF2cmEotvkt3dTRQ+4wcKfaMqQ==",
            :private-key "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDUKuVmLhSVupWDzT208DFnW3n9ebx1Hl/2vgk3tnjMRg=="}
        aid #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"

        cl (auth-client (dissoc ti :private-key) aid 100 #(Date.))
        se (auth-server ti 800 #(Date.)
                        (fn get-user-info [ticket-request on-success on-failure]
                          (on-success #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"
                                      {:name "U Ser"})))

        ticket-request (cl)
        ticket (se ticket-request)]
    (testing "ticket codec"
      (is (= ticket (decode-ticket (encode-ticket ticket)))))
    (testing "expiration"
      (is (s/valid? ::sut/granted-ticket (cl ticket)))
      (Thread/sleep 120)
      (is (= :expired (:failure-reason (cl ticket)))))
    (testing "grant failures"
      (is (= {:name "U Ser"} (:user-info ticket)))
      (is (= :future-request
             (:failure-reason
              (se (assoc ticket-request :request-timestamp (Date. (+ (System/currentTimeMillis) 50)))))))
      (is (= :expired-request
             (:failure-reason
              (se (assoc ticket-request :wanted-session-end-timestamp (Date. (- (System/currentTimeMillis) 50))))))))))

(deftest test-parse-header
  (are [hv kvs] (= (parse-header (str "Webnf " hv)) ["Webnf" kvs])
    "foo=\"bar\"" {"foo" "bar"}
    "foo =\"bar\", moo= \"ma\" " {"foo" "bar" "moo" "ma"}
    "foo  = \"b\\\"ar\", moo=\"m\\\\a\"" {"foo" "b\"ar" "moo" "m\\a"})
  (are [hv] (nil? (parse-header (str "Webnf " hv)))
    "" "foo=bar" "foo=\"foo\"bar\"" "moo=\"m\\o\""))

(defmethod assert-expr 'http-success? [msg form]
  `(let [{status# :status :as resp#} (do ~@(next form))]
     (if (and status# (<= 200 status# 299))
       (do (do-report {:type :pass :message ~msg
                       :expected '{:status "2**" ... ...}
                       :actual resp#})
           resp#)
       (do-report {:type :fail :message ~msg
                   :expected '{:status "2**" ... ...}
                   :actual resp#}))))

(deftest client-server-handlers
  (let [ti {:public-key "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE4Wt9keq71vtffMK2KtUfTBb3dvvg7dvGN3dXiLlS4zFTAbL+nap55aHhmpVKTF2cmEotvkt3dTRQ+4wcKfaMqQ==",
            :private-key "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDUKuVmLhSVupWDzT208DFnW3n9ebx1Hl/2vgk3tnjMRg=="}
        aid #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"

        cl (auth-client (dissoc ti :private-key) aid 1000 #(Date.))
        se (auth-server ti 800 #(Date.)
                        (fn get-user-info [ticket-request on-success on-failure]
                          (on-success #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"
                                      {:name "U Ser"})))
        server (server-handler se)
        client (-> (wrap-protect (constantly {:status 200})
                                 :goto "https://test.com/login")
                   (wrap-client cl))

        not-auth-resp (client (mock/request :get "/"))
        _ (do (is (= (:status not-auth-resp) 401)))
        [method {:strs [token goto]}] (parse-header (rur/get-header not-auth-resp "www-authenticate"))
        _ (do (is (= method "Webnf-Ticket"))
              (is (= goto "https://test.com/login")))
        auth-server-resp (server (-> (mock/request :post "/")
                                     (mock/content-type "application/fressian+base64")
                                     (mock/body token)))
        _ (do (is (= (:status auth-server-resp) 200))
              (is (= (rur/get-header auth-server-resp "content-type") "application/fressian+base64")))]
    (is (http-success? (-> (mock/request :get "/")
                           (mock/header "authorization" (str "Webnf-Ticket " (:body auth-server-resp)))
                           client)))
    (is (http-success? (-> (mock/request :get "/")
                           (mock/header "x-webnf-auth-ticket" (:body auth-server-resp))
                           client)))
    (is (http-success? (-> (mock/request :get "/")
                           (mock/header "cookie" (str "webnf-auth-ticket=" (:body auth-server-resp)))
                           client)))))
