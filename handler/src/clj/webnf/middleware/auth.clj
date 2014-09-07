(ns webnf.middleware.auth
  (:require
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [ring.util.codec :as codec]))

(defn login-prompt-response [prompt]
  {:status 401
   :headers {"WWW-Authenticate" (format "Basic realm=\"%s\"" prompt)}})

(defn wrap-basic-auth [handler attach-auth require-auth]
  (fn [{{auth "authorization"} :headers
        :keys [server-name]
        :as req}]
    (if-not auth
      (if require-auth
        (login-prompt-response server-name)
        (handler req))
      (let [[auth-type cred] (str/split auth #" ")]
        (if-not (= auth-type "Basic")
          {:status 501 :body "Only Basic AUTH supported"}
          (let [[user pw] (str/split (String. (codec/base64-decode cred) "UTF-8")
                                     #":")]
            (if-not (string? user)
              {:status 400 :body "Invalid Basic AUTH header"}
              (attach-auth handler req user pw))))))))

