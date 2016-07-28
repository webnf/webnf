(ns webnf.handler.auth
  "Asymmetric single sign-on ticketing

  Ticket Server only does user verification and timestamping. Ticket
  Client verifies signature to establish user identity (as per Ticket
  Server policy). Ticket Client then establishes user capabilities (as
  roles and grants) from its own data source."
  (:require
   [clojure.edn :as edn]
   [clojure.spec :as s]
   [webnf.handler.auth.crypto :as crypt]
   [webnf.handler.auth.codec :as codec]
   [clojure.test :refer :all])
  (:import (java.util Date)))

(s/def ::app-id uuid?)
(s/def ::description string?)
(s/def ::roles  (s/coll-of keyword? :into #{}))
(s/def ::grants (s/coll-of (s/cat :grant keyword? :param any?) :into #{}))
(s/def ::app-info
  (s/keys :opt-un [::roles ::grants ::description]))
(s/def ::request-timestamp inst?)
(s/def ::wanted-session-end-timestamp inst?)
(s/def ::user-id uuid?)
(s/def ::name string?)
(s/def ::user-info
  (s/keys :opt-un [::name]))
(s/def ::grant-start-timestamp inst?)
(s/def ::grant-end-timestamp inst?)
(s/def ::authentication-signature ::crypt/signature)
(s/def ::message string?)
(s/def ::failure-reason
  (s/or :message ::message
        :info (s/keys :req-un [::message])))

(s/def ::ticket-request
  (s/keys :req-un [::app-id ::request-timestamp ::wanted-session-end-timestamp]))

(defrecord TicketRequest
    [app-id request-timestamp wanted-session-end-timestamp])

(s/def ::granted-ticket
  (s/keys :req-un [::user-id ::user-info ::app-id
                   ::grant-start-timestamp ::grant-end-timestamp
                   ::authentication-signature]))

(defrecord GrantedTicket
    [authentication-signature
     user-id user-info app-id
     grant-start-timestamp grant-end-timestamp])

(s/def ::grant-failure
  (s/keys :req-un [::ticket-request ::failure-reason]))

(defrecord GrantFailure
    [ticket-request failure-reason])

(defrecord GrantInvalidFailure
    [failure-reason ticket])

(s/fdef ::sign-grant
        :args (s/cat :key ::crypt/key-pair :request ::ticket-request)
        :ret (s/or :success ::granted-ticket
                   :failure ::grant-failure))

(defn sign-grant [auth-identity user-id user-info grant-start-timestamp grant-end-timestamp
                  {:keys [app-id request-timestamp wanted-session-end-timestamp]}]
  (let [sig-data [user-id user-info app-id grant-start-timestamp grant-end-timestamp]]
    (apply ->GrantedTicket
           (crypt/sign auth-identity sig-data)
           sig-data)))

(s/fdef ::verify-grant-signature
        :args (s/cat :key ::crypt/key-pair-public-part :ticket ::granted-ticket)
        :ret boolean?)

(defn grant-signature-valid? [auth-pub-identity
                              {:keys [authentication-signature
                                      user-id user-info app-id
                                      grant-start-timestamp grant-end-timestamp]}]
  (crypt/verify auth-pub-identity [user-id user-info app-id
                                   grant-start-timestamp grant-end-timestamp]
                authentication-signature))

(defn date< [d1 d2]
  (neg? (compare d1 d2)))

(defn date> [d1 d2]
  (pos? (compare d1 d2)))

(deftest date-comparison
  (is (date< #inst "2015" #inst "2016"))
  (is (date> #inst "2016" #inst "2015"))
  (is (not (date< #inst "2015" #inst "2015")))
  (is (not (date> #inst "2015" #inst "2015"))))

(defn validate-grant [auth-pub-identity timestamp expect-app-id
                      {:as ticket
                       :keys [authentication-signature
                              user-id user-info app-id
                              grant-start-timestamp grant-end-timestamp]}]
  (cond
    (not= expect-app-id app-id)
    (->GrantInvalidFailure :app-mismatch ticket)
    (date< timestamp grant-start-timestamp)
    (->GrantInvalidFailure :not-yet-valid ticket)
    (date> timestamp grant-end-timestamp)
    (->GrantInvalidFailure :expired ticket)
    (grant-signature-valid? auth-pub-identity ticket)
    ticket
    :else
    (->GrantInvalidFailure :signature-mismatch ticket)))

(let [to-seq (juxt (comp codec/dec64 :authentication-signature)
                   :app-id :user-id :user-info
                   :grant-start-timestamp
                   :grant-end-timestamp)
      from-seq (fn [as aid uid uinf gst get]
                 (->GrantedTicket (codec/enc64 as)
                                  uid uinf
                                  aid gst get))]
  (defn encode-ticket [t]
    (codec/encode-args t :to-seq to-seq))

  (defn decode-ticket [et]
    (codec/decode-args et :from-seq from-seq)))

(let [to-seq (juxt :app-id :request-timestamp :wanted-session-end-timestamp)
      from-seq ->TicketRequest]
  (defn encode-ticket-request [tr]
    (codec/encode-args tr :to-seq to-seq))
  (defn decode-ticket-request [etr]
    (codec/decode-args etr :from-seq from-seq)))

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
    (is (s/valid? ::granted-ticket
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

(defn auth-client [auth-pub-identity expect-app-id ticket-exp-duration get-timestamp]
  (fn
    ([] (let [cur-ts (get-timestamp)]
          (->TicketRequest expect-app-id cur-ts (Date. (+ ticket-exp-duration (.getTime cur-ts))))))
    ([ticket] (validate-grant auth-pub-identity (get-timestamp) expect-app-id ticket))))

(defn auth-server [auth-identity max-expire-duration get-timestamp get-user-info]
  (fn [{:as ticket-request :keys [request-timestamp wanted-session-end-timestamp]}]
    (let [cur-ts (get-timestamp)]
      (cond
        (date> request-timestamp cur-ts)
        (->GrantFailure ticket-request :future-request)
        (date> cur-ts wanted-session-end-timestamp)
        (->GrantFailure ticket-request :expired-request)
        :else
        (get-user-info
         (fn on-success [user-id user-info]
           (let [expire-duration (min max-expire-duration
                                      (- (.getTime wanted-session-end-timestamp)
                                         (.getTime request-timestamp)
                                         (- (.getTime cur-ts)
                                            (.getTime request-timestamp))))]
             (sign-grant auth-identity user-id user-info
                         cur-ts (Date. (+ (.getTime cur-ts) expire-duration))
                         ticket-request)))
         (fn on-failure [reason] (->GrantFailure ticket-request reason)))))))

(deftest grant-client-server
  (let [ti {:public-key "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE4Wt9keq71vtffMK2KtUfTBb3dvvg7dvGN3dXiLlS4zFTAbL+nap55aHhmpVKTF2cmEotvkt3dTRQ+4wcKfaMqQ==",
            :private-key "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDUKuVmLhSVupWDzT208DFnW3n9ebx1Hl/2vgk3tnjMRg=="}
        aid #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"

        cl (auth-client (dissoc ti :private-key) aid 100 #(Date.))
        se (auth-server ti 800 #(Date.)
                        (fn get-user-info [on-success on-failure]
                          (on-success #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"
                                      {:name "U Ser"})))

        ticket-request (cl)
        ticket (se ticket-request)]
    (testing "ticket codec"
      (is (= ticket (decode-ticket (encode-ticket ticket)))))
    (testing "expiration"
      (is (s/valid? ::granted-ticket (cl ticket)))
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

(defn failure? [m]
  (contains? m :failure-reason))

(defmulti failure-response :failure-reason)

(defmethod failure-response :default
  [{tr :ticket-request fr :failure-reason}]
  {:status 400 :body (pr-str [fr tr])})

(defn server-handler
  "Construct ring handler for signing tickets
   Takes POST request with ticket requests encoded as application/fressian+base64

   ticket-server: a signing function, as per `auth-server`"
  [ticket-server]
  (fn [{:keys [request-method headers body]}]
    (cond (not= :post request-method)
          {:status 405}
          (not= "application/fressian+base64" (get headers "content-type"))
          {:status 406}
          (> 10000 (Long/parseLong (get headers "content-length")))
          {:status 400 :body "Size limit exceeded"}
          :else
          (let [ticket-req (decode-ticket-request (slurp body))
                grant (ticket-server ticket-req)]
            (if (failure? grant)
              (failure-response grant)
              {:status 200
               :headers {"content-type" "application/fressian+base64"}
               :body (encode-ticket grant)})))))
   

(defn wrap-client [h ticket-client
                   & {:keys [ticket-server-uri ]}]
  (comment
    (app
     [app-uuid &] [["set-cookie"] (call back from ticket server with)])))
