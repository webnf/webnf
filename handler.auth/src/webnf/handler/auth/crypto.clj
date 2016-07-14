(ns webnf.handler.auth.crypto
  (:require
   [hasch.benc :as benc]
   [hasch.platform :refer [sha512-message-digest]]
   [webnf.handler.auth.codec :as codec])
  (:import
   (java.security KeyPairGenerator KeyFactory Signature SecureRandom
                  KeyPair)
   (java.security.spec X509EncodedKeySpec PKCS8EncodedKeySpec)))

(defonce sr (SecureRandom/getInstance "SHA1PRNG"))
(defonce kg (doto (KeyPairGenerator/getInstance "EC")
              (.initialize 256 sr)))
(defonce kf (KeyFactory/getInstance "EC"))

(defn- as-public [b64-pk]
  (.generatePublic kf (X509EncodedKeySpec. (codec/dec64 b64-pk))))
(defn- as-private [b64-pk]
  (.generatePrivate kf (PKCS8EncodedKeySpec. (codec/dec64 b64-pk))))

(defn- as-kp [{:keys [pub priv]}]
  (KeyPair.
   (as-public pub)
   (as-private priv)))

(defn- as-pp [^KeyPair key-pair]
  {:pub (codec/enc64 (.getEncoded (.getPublic key-pair)))
   :priv (codec/enc64 (.getEncoded (.getPrivate key-pair)))})

(defn gen-identity []
  (as-pp (.generateKeyPair kg)))

(defn sign [{:keys [priv]} data]
  (let [sig (doto (Signature/getInstance "SHA512withECDSA")
              (.initSign (as-private priv)))]
    (.update sig (benc/-coerce data sha512-message-digest {}))
    (codec/enc64 (.sign sig))))

(defn verify [{:keys [pub]} data signature]
  (let [sig (doto (Signature/getInstance "SHA512withECDSA")
              (.initVerify (as-public pub)))]
    (.update sig (benc/-coerce data sha512-message-digest {}))
    (.verify sig (codec/dec64 signature))))
