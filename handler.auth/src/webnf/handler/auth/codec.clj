(ns webnf.handler.auth.codec
  (:import
   java.nio.ByteBuffer
   java.util.Base64
   javax.xml.bind.DatatypeConverter
   (java.io ByteArrayOutputStream ByteArrayInputStream)
   (java.util.zip Deflater Inflater DeflaterOutputStream InflaterInputStream))
  (:require
   [clojure.data.fressian :as fress]))

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

(defn encode-args [data & {:keys [to-seq compress]
                           :or {to-seq seq
                                compress false}}]
  (let [bos (ByteArrayOutputStream.)]
    (with-open [wrt (fress/create-writer
                     (if compress
                       (DeflaterOutputStream. bos (Deflater. Deflater/BEST_COMPRESSION true) true)
                       bos))]
      (fress/write-object wrt (to-seq data)))
    (enc64 (.toByteArray bos))))

(defn decode-args [encoded & {:keys [from-seq compress]
                              :or {from-seq list
                                   compress false}}]
  (let [bis (ByteArrayInputStream. (dec64 encoded))]
    (with-open [rdr (fress/create-reader
                     (if compress
                       (InflaterInputStream. bis (Inflater. true))
                       bis))]
      (apply from-seq (fress/read-object rdr)))))

(comment
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
       #_(webnf.handler.auth/->Ticket
          (fress/read-object rdr)
          (fress/read-object rdr)
          (fress/read-object rdr)
          (fress/read-object rdr)
          (fress/read-object rdr)
          (fress/read-object rdr)
          (enc64 (fress/read-object rdr)))))))
