(ns webnf.handler.auth
  "Asymmetric single sign-on ticketing

  Ticket Server only does user verification and timestamping. Ticket
  Client verifies signature to establish user identity (as per Ticket
  Server policy). Ticket Client then establishes user capabilities (as
  roles and grants) from its own data source."
  (:require
   [webnf.handler.auth.crypto :as crypt]
   [webnf.handler.auth.codec :as codec]
   [clojure.test :refer :all]))

(defn sign-ticket [identity user-id user-name timestamp app-id roles grants]
  (crypt/sign identity [user-id user-name timestamp app-id roles grants]))

(defn verify-ticket [identity signature user-id user-name timestamp app-id roles grants]
  (crypt/verify identity [user-id user-name timestamp app-id roles grants] signature))

(defrecord Success [user-id user-name expire-timestamp roles grants])
(defrecord Failure [reason ticket])

(defn success? [res] (instance? Success res))
(defn failure? [res] (instance? Failure res))

(defn auth-client [ticket-server-id here-app-id ticket-exp-duration get-timestamp]
  (fn [{:keys [user-id user-name timestamp signature app-id roles grants] :as ticket}]
    (let [cur-ts (get-timestamp)]
      (cond
        (not= here-app-id app-id)
        (->Failure :client-mismatch ticket)
        (> cur-ts (+ timestamp ticket-exp-duration))
        (->Failure :expired ticket)
        (verify-ticket ticket-server-id signature user-id user-name timestamp app-id roles grants)
        (->Success user-id user-name (+ timestamp ticket-exp-duration) roles grants)
        :else
        (->Failure :signature-mismatch ticket)))))

(defrecord Ticket [user-id user-name timestamp app-id roles grants signature])

(defn auth-server [ticket-server-id get-timestamp]
  (fn [user-id user-name app-id roles grants]
    (let [ts (get-timestamp)]
      (->Ticket
       user-id user-name ts app-id roles grants
       (sign-ticket ticket-server-id user-id user-name
                    ts app-id roles grants)))))

(comment

  (def i (crypt/gen-identity))

  (def s (auth-server i #(System/currentTimeMillis)))
  (def c (auth-client i #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"
                      1000000
                      #(System/currentTimeMillis)))

  (def ticket (s #uuid "54100d61-7e85-4266-b0eb-e52ffcb79055" "Herwig Hochleitner" #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b" #{:readers} #{[:write "foo"]}))
  (c ticket)
  
  )

(deftest ticket-success
  (let [i (crypt/gen-identity)

        s (auth-server i #(System/currentTimeMillis))
        c (auth-client i #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"
                       500
                       #(System/currentTimeMillis))
        ticket (s #uuid "54100d61-7e85-4266-b0eb-e52ffcb79055" "Herwig Hochleitner" #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b" #{:readers} #{[:write "foo"]})]
    (is (success? (c ticket)))
    (Thread/sleep 1000)
    (is (failure? (c ticket)))))


(comment
  (defn server-handler
    "Params:
    ticket-server-id : UUID of this service
    get-timestamp : Timestamp provider, a function of no args, returning milliseconds
    get-client-id : Authentication function, [user-name password app-id] -> "
    [ticket-server-id get-timestamp get-client-id]
    (fn [req]
      )))
