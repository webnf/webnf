(ns webnf.crypto
  (:import
   java.security.SecureRandom
   (com.lambdaworks.crypto SCrypt SCryptUtil)
   com.lambdaworks.codec.Base64)
  (:require [clojure.string :as str]))

;; Reimplement com.lambdawerks.crypto.SCryptUtil
;; in order to be able to use the pure-java impl
;; due to segfaults in the native lib

(defn log2 [n]
  (- 63 (Long/numberOfLeadingZeros n)))

(defn pull-rand! [^bytes ba]
  (-> (SecureRandom/getInstance "SHA1PRNG")
      (.nextBytes ba)))

(defn scrypt [passwd N r p]
  (let [salt (doto (byte-array 16)
               pull-rand!)
        derived (SCrypt/scryptJ (.getBytes passwd "UTF-8")
                                salt N r p 32)
        params (-> N log2
                   (bit-shift-left 16)
                   (bit-or (bit-shift-left r 8)
                           p)
                   (Long/toString 16))]
    (-> (StringBuilder. (* 2 (+ (alength salt) (alength derived))))
        (doto #__
          (.append "$s0$")
          (.append params)
          (.append \$)
          (.append (Base64/encode salt))
          (.append \$)
          (.append (Base64/encode derived)))
        .toString)))

(defn check [passwd hash]
  (let [[_ marker params' salt' derived0']
        (str/split hash #"\$")]
    (assert (and (= marker "s0") params' salt' derived0') (str "Illegal hash: " hash))
    (let [params   (Long/parseLong params' 16)
          N (Math/pow 2 (bit-and 0xffff (bit-shift-right params 16)))
          r (bit-and 0xff (bit-shift-right params 8))
          p (bit-and 0xff params)
          salt     (Base64/decode (.toCharArray salt'))
          derived0 (Base64/decode (.toCharArray derived0'))
          derived1 (SCrypt/scryptJ (.getBytes passwd "UTF-8")
                                   salt N r p 32)]
      (zero? (reduce (fn [^long n i]
                       (bit-or n (bit-xor (aget derived0 i)
                                          (aget derived1 i))))
                     0 (range (alength derived0)))))))

(defn valid-password? [hash password]
  (when (and hash password)
    (check password hash)))

(defn password-hash [password]
  (scrypt password 16384 8 4))
