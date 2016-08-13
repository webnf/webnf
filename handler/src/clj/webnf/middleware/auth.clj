(ns webnf.middleware.auth
  (:require
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [ring.util.codec :as codec]))

(defn login-prompt-response [prompt]
  {:status 401
   :headers {"WWW-Authenticate" (format "Basic realm=\"%s\"" prompt)}})

(defn wrap-basic-auth
  "Wrap handler with a basic auth mechanism
  Configuration:
    attach-auth is a (fn [handler request username password])
        which is called to further handle the request.
        Default action: Call handler with the request
            + added keys :webnf.middleware.auth/username
                     and :webnf.middleware.auth/password
    require-auth is a boolean, determining whether auth needs to be provided
        If require-auth is a string, that string is used for the login prompt (realm)"
  [handler & {:keys [attach-auth require-auth]
              :or {attach-auth (fn [h r u p]
                                 (h (assoc r ::username u ::password p)))}}]
  (fn [{{auth "authorization"} :headers
        :keys [server-name]
        :as req}]
    (if-not auth
      (if require-auth
        (login-prompt-response (if (string? require-auth)
                                 require-auth
                                 server-name))
        (handler req))
      (let [[auth-type cred] (str/split auth #" ")]
        (if-not (= auth-type "Basic")
          {:status 501 :body "Only Basic AUTH supported"}
          (let [[user pw] (str/split (String. (codec/base64-decode cred) "UTF-8")
                                     #":")]
            (attach-auth handler req (or user "") (or pw ""))))))))

