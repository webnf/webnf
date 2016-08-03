(ns webnf.handler.auth
  "Asymmetric single sign-on ticketing

  Ticket Server only does user verification and timestamping. Ticket
  Client verifies signature to establish user identity (as per Ticket
  Server policy). Ticket Client then establishes user capabilities (as
  roles and grants) from its own data source."
  (:require
   [webnf.base :refer [str-quote]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [clojure.edn :as edn]
   [clojure.spec :as s]
   [webnf.handler.auth.crypto :as crypt]
   [webnf.handler.auth.codec :as codec]
   [instaparse.core :as insta]
   [clojure.string :as str]
   [clojure.tools.logging :as log])
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

(def failure? :failure-reason)

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
         ticket-request
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

(defn failure? [m]
  (contains? m :failure-reason))

(defmulti failure-response :failure-reason)

(defmethod failure-response :default
  [{tr :ticket-request fr :failure-reason}]
  {:status 400 :body (pr-str [fr tr])})

(def auth-header-parser
  (insta/parser
   "<HEADER> = PREFIX (KVPAIR <#'\\s*,\\s*'>)* KVPAIR
    <PREFIX> = #'\\S+' <#'\\s+'>
    <KVPAIR> = KEY <#'\\s*=\\s*\"'> VALUE <#'\"\\s*'>
    <KEY>    = #'[^=\\s]+'
    <VALUE>  = #'(?:[^\"\\\\]|\\\\\"|\\\\\\\\)+'"))

(defn parse-header [hv]
  (let [res (insta/parse auth-header-parser hv)]
    (when-not (insta/failure? res)
      (loop [tres (transient {})
             [k v & rst :as kvs] (next res)]
        (if kvs
          (recur (assoc! tres k (str/replace v #"\\\"|\\\\" #(case % "\\\"" "\"" "\\\\" "\\")))
                 rst)
          [(first res) (persistent! tres)])))))

(defn server-handler
  "Construct ring handler for signing tickets
   Takes POST request with ticket requests encoded as application/fressian+base64

   ticket-server: a signing function, as per `auth-server`"
  [ticket-server & {:keys [max-content-length]}]
  (fn [{{:strs [content-length content-type]} :headers
        :keys [request-method body]
        :as req}]
    (cond (not= :post request-method)
          {:status 405}
          (not (contains? #{"application/fressian+base64"
                            "application/www-authenticate"}
                          content-type))
          {:status 406 :body "accepted: application/fressian+base64, application/www-authenticate"}
          (and max-content-length
               (< max-content-length (Long/parseLong content-length)))
          {:status 400 :body (str "Max Content-Length exceeded: " content-length " > " max-content-length)}
          :else
          (let [ticket-req (case content-type
                             "application/www-authenticate"
                             (let [[method {:strs [token]} :as foo]
                                   (parse-header (slurp body))]
                               (when (= "Webnf-Ticket" method)
                                 (decode-ticket-request token)))
                             "application/fressian+base64"
                             (decode-ticket-request (slurp body)))
                grant (ticket-server (assoc ticket-req ::request req))]
            (if (failure? grant)
              (failure-response grant)
              {:status 200
               :headers {"content-type" "application/fressian+base64"}
               :body (encode-ticket grant)})))))

(defn parse-auth-header [hdr]
  (and hdr (second (re-find #"Webnf-Ticket (.*)" hdr))))

(defn wrap-client [h ticket-client]
  (-> (fn [{:as req
            {auth-header "authorization"
             x-auth-header "x-webnf-auth-ticket"} :headers
            {{cookie-auth :value} "webnf-auth-ticket"} :cookies}]
        (let [token (or x-auth-header cookie-auth
                        (parse-auth-header auth-header))]
          (-> req
              (assoc ::ticket-requester (reify clojure.lang.IDeref
                                          ;; encode freshly created ticket
                                          (deref [_] (encode-ticket-request (ticket-client))))
                     ::ticket nil ::auth-error nil)
              (cond-> token (as-> req
                                (let [ticket (ticket-client (decode-ticket token))]
                                  (if (failure? ticket)
                                    (assoc req ::auth-error ticket)
                                    (assoc req ::ticket ticket)))))
              h)))
      wrap-cookies))

(defn print-www-auth-kv [scheme kvs]
  (apply str scheme " " (apply concat
                               (interpose
                                [", "]
                                (for [[k v] kvs
                                      :when v]
                                  [k "=" (str-quote v)])))))

(defn wrap-protect [h & {:keys [on-success realm goto]}]
  (fn [{:as req :keys [::ticket-requester ::auth-error ::ticket]}]
    (if ticket
      (if on-success
        (on-success h req ticket)
        (h req))
      {:status 401
       :headers {"WWW-Authenticate" (print-www-auth-kv "Webnf-Ticket" {"realm" realm
                                                                       "token" @ticket-requester
                                                                       "error" (:failure-reason auth-error)
                                                                       "goto"  goto})}})))

