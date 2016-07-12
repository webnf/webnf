(ns webnf.handler.auth
  "Asymmetric single sign-on ticketing

  Ticket Server only does user verification and timestamping. Ticket
  Client verifies signature to establish user identity (as per Ticket
  Server policy). Ticket Client then establishes user capabilities (as
  roles and grants) from its own data source."
  (:require
   [webnf.handler.auth.crypto :as crypt]
   [clojure.test :refer :all]))

(defn sign-ticket* [identity user-id user-name timestamp client-id]
  (crypt/sign identity [user-id user-name timestamp client-id]))

(defn verify-ticket [identity signature user-id user-name timestamp client-id]
  (crypt/verify identity [user-id user-name timestamp client-id] signature))

(defrecord Success [user-id user-name expire-timestamp roles grants])
(defrecord Failure [reason ticket])

(defn success? [res] (instance? Success res))
(defn failure? [res] (instance? Failure res))

(defn auth-client [ticket-server-id here-client-id ticket-exp-duration
                   user-roles user-grants get-timestamp]
  (fn [{:keys [user-id user-name timestamp signature client-id] :as ticket}]
    (let [cur-ts (get-timestamp)]
      (cond
        (not= here-client-id client-id)
        (->Failure :client-mismatch ticket)
        (> cur-ts (+ timestamp ticket-exp-duration))
        (->Failure :expired ticket)
        (crypt/verify ticket-server-id [user-id user-name timestamp client-id] signature)
        (->Success user-id user-name (+ timestamp ticket-exp-duration)
                   (user-roles user-id) (user-grants user-id))
        :else
        (->Failure :signature-mismatch ticket)))))

(defrecord Ticket [user-id user-name timestamp client-id signature])

(defn auth-server [ticket-server-id get-timestamp]
  (fn [user-id user-name client-id]
    (let [ts (get-timestamp)]
      (->Ticket
       user-id user-name ts client-id
       (crypt/sign ticket-server-id [user-id user-name ts client-id])))))

(comment

  (def i (crypt/gen-identity))

  (def s (auth-server i
                      #(System/currentTimeMillis)))
  (def c (auth-client i #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"
                      10000 (constantly #{:readers}) (constantly #{[:write "foo"]})
                      #(System/currentTimeMillis)))

  (def ticket (s #uuid "54100d61-7e85-4266-b0eb-e52ffcb79055" "Herwig Hochleitner" #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"))
  (c ticket)
  
  )

(deftest ticket-success
  (let [i (crypt/gen-identity)

        s (auth-server i
                       #(System/currentTimeMillis))
        c (auth-client i #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b"
                       500 (constantly #{:readers}) (constantly #{[:write "foo"]})
                       #(System/currentTimeMillis))
        ticket (s #uuid "54100d61-7e85-4266-b0eb-e52ffcb79055" "Herwig Hochleitner" #uuid "4aafa30f-e8a6-4364-9610-d00700fd773b")]
    (is (success? (c ticket)))
    (Thread/sleep 1000)
    (is (failure? (c ticket)))))

(comment
  (defn server-handler
    "Params:
    ticket-server-id : UUID of this service
    get-timestamp : Timestamp provider, a function of no args, returning milliseconds
    get-client-id : Authentication function, [user-name password client-id] -> "
    [ticket-server-id get-timestamp get-client-id]
    (fn [req]
      )))
