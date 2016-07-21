(ns webnf.handler.auth.crypto
  (:require
   [hasch.benc :as benc]
   [hasch.platform :refer [sha512-message-digest]]
   [webnf.handler.auth.codec :as codec]
   [clojure.spec :as s])
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

(defn- as-kp [{:keys [public-key private-key]}]
  (KeyPair.
   (as-public public-key)
   (as-private private-key)))

(defn- as-pp [^KeyPair key-pair]
  {:public-key (codec/enc64 (.getEncoded (.getPublic key-pair)))
   :private-key (codec/enc64 (.getEncoded (.getPrivate key-pair)))})

(s/def ::public-key (s/and
                     string?
                     (s/conformer #(try (as-public %)
                                        (catch Exception e ::s/invalid)))))
(s/def ::private-key (s/and
                      string?
                      (s/conformer #(try (as-private %)
                                         (catch Exception e ::s/invalid)))))

(s/def ::key-pair (s/keys :req-un [::public-key ::private-key]))
(s/def ::key-pair-public-part (s/keys :req-un [::public-key]))

(s/def ::signature
  (s/and string? #(codec/dec64 %)))

(s/fdef ::gen-identity :ret ::key-pair)

(defn gen-identity []
  (as-pp (.generateKeyPair kg)))

(s/fdef ::sign
        :args (s/cat :key-pair ::key-pair
                     :data any?)
        :ret ::signature)

(defn sign [{:keys [private-key]} data]
  (let [sig (doto (Signature/getInstance "SHA512withECDSA")
              (.initSign (as-private private-key)))]
    (.update sig (benc/-coerce data sha512-message-digest {}))
    (codec/enc64 (.sign sig))))

(s/fdef ::verify
        :args (s/cat :key-pair ::key-pair-public-part
                     :data any?
                     :signature ::signature)
        :ret boolean?)

(defn verify [{:keys [public-key]} data signature]
  (let [sig (doto (Signature/getInstance "SHA512withECDSA")
              (.initVerify (as-public public-key)))]
    (.update sig (benc/-coerce data sha512-message-digest {}))
    (.verify sig (codec/dec64 signature))))
