(ns webnf.handler.auth.codec
  (:import
   java.nio.ByteBuffer
   java.util.Base64
   javax.xml.bind.DatatypeConverter
   (java.io ByteArrayOutputStream ByteArrayInputStream)
   (java.util.zip Deflater Inflater DeflaterOutputStream InflaterInputStream))
  (:require
   [clojure.data.fressian :as fress]
   [clojure.test :refer [deftest are is]]))

(defn enc16 [ba]
  (.toLowerCase
   (DatatypeConverter/printHexBinary ba)))

(defn dec16 [hba]
  (DatatypeConverter/parseHexBinary hba))

(defn enc64 [ba]
  (String.
   (.. Base64 getEncoder (encode ba))
   "UTF-8"))

(defn dec64 [bba]
  (.. Base64 getDecoder (decode bba)))

(defn encode-object [object]
  (let [bos (ByteArrayOutputStream.)]
    (with-open [wrt (fress/create-writer bos #_(DeflaterOutputStream. bos (Deflater. Deflater/BEST_COMPRESSION true) true))]
      (fress/write-object wrt object))
    (enc64 (.toByteArray bos))))

(defn decode-object [object-str]
  (let [bis (ByteArrayInputStream. (dec64 object-str))]
    (with-open [rdr (fress/create-reader bis #_(InflaterInputStream. bis (Inflater. true)))]
      (fress/read-object rdr))))

(deftest codec-roundtrip
  (defrecord Foo [a b])
  (are [same] (= same (decode-object (encode-object same)))
    {:ticket 1}
    (->Foo 1 2)))

(defn encode-ticket [{:keys [user-id user-name timestamp app-id roles grants signature]}]
  (let [bos (ByteArrayOutputStream.)]
    (with-open [wrt (fress/create-writer bos #_(DeflaterOutputStream. bos (Deflater. Deflater/BEST_COMPRESSION true) true))]
      (fress/write-object wrt user-id)
      (fress/write-object wrt user-name)
      (fress/write-object wrt timestamp)
      (fress/write-object wrt app-id)
      (fress/write-object wrt roles)
      (fress/write-object wrt grants)
      (fress/write-object wrt (dec64 signature)))
    (enc64 (.toByteArray bos))))

(defn decode-ticket [ticket-str]
  (let [bis (ByteArrayInputStream. (dec64 ticket-str))]
    (with-open [rdr (fress/create-reader bis #_(InflaterInputStream. bis (Inflater. true)))]
      (webnf.handler.auth/->Ticket
       (fress/read-object rdr)
       (fress/read-object rdr)
       (fress/read-object rdr)
       (fress/read-object rdr)
       (fress/read-object rdr)
       (fress/read-object rdr)
       (enc64 (fress/read-object rdr))))))
