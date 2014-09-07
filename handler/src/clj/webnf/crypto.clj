(ns webnf.crypto
  (:import com.lambdaworks.crypto.SCryptUtil))

(defn valid-password? [hash password]
  (when (and hash password)
    (SCryptUtil/check password hash)))

(defn password-hash [password]
  (SCryptUtil/scrypt password 16384 8 4))
