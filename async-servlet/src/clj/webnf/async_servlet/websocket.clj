(ns webnf.async-servlet.websocket
  (:require [clojure.core.async :refer [chan go go-loop thread <! >! <!! >!! close! alt!]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [webnf.base :refer [static-case]]
            [webnf.async :refer [rw-chan]]
            [webnf.async-servlet.UpgradeHandler :refer [*init*]])
  (:import (java.io Writer InputStream File DataOutputStream)
           (java.nio ByteBuffer)
           (java.nio.channels Channels WritableByteChannel)
           java.security.MessageDigest
           java.util.Base64
           (javax.servlet ServletInputStream ServletOutputStream ReadListener WriteListener)
           (javax.servlet.http WebConnection HttpUpgradeHandler HttpServletRequest HttpServletResponse)
           webnf.async_servlet.UpgradeHandler
           (webnf.ws Decoder Util FrameCallsite)))

(defprotocol Frame
  (encode [frame stream]))

(defrecord TextFrame [message]
  Frame (encode [_ stream] (Util/encodeFrame stream Decoder/OPCODE_TEXT (.getBytes ^String message "UTF-8"))))
(defrecord BinaryFrame [data]
  Frame (encode [_ stream] (Util/encodeFrame stream Decoder/OPCODE_BINARY data)))
(defrecord PingFrame [data]
  Frame (encode [_ stream] (Util/encodeFrame stream Decoder/OPCODE_PING data)))
(defrecord PongFrame [data]
  Frame (encode [_ stream] (Util/encodeFrame stream Decoder/OPCODE_PONG data)))
(defrecord CloseFrame [status]
  Frame (encode [_ stream] (Util/encodeFrame stream Decoder/OPCODE_CLOSE (.array (doto (ByteBuffer/allocate 2)
                                                                                   (.writeShort status))))))

(def frame-creator
  (reify FrameCallsite
    (invoke [_ opcode data]
      (static-case opcode
                   Decoder/OPCODE_TEXT (->TextFrame (String. data "UTF-8"))
                   Decoder/OPCODE_BINARY (->BinaryFrame data)
                   Decoder/OPCODE_PING (->PingFrame data)
                   Decoder/OPCODE_PONG (->PongFrame data)
                   Decoder/OPCODE_CLOSE (->CloseFrame (.getShort (ByteBuffer/wrap data 0 2)))))))

(defn- parse-try-arm [try arm]
  (let [[op cls arg] (when (seq? arm) arm)]
    (case op
      catch (assoc-in try [:catch cls] arm)
      finally (do (assert (not (:finally try)) "Multiple finally clauses")
                  (assoc try :finally arm))
      (update try :body (fnil conj []) arm))))

(defn- emit-catch [{catches :catch}]
  (vals catches))

(defn- emit-finally [{thunk :finally}]
  (when thunk [thunk]))

(defmacro with-boundary [desc & body+clauses]
  (let [{body :body :as try}
        (reduce parse-try-arm nil body+clauses)]
    `(try (log/trace "ENTERING" ~desc)
          (let [res# (do ~@body)]
            (log/trace "EXITING" ~desc "with results" res#)
            res#)
          ~@(or (seq (emit-catch try))
                [`(catch Exception e#
                    (log/error e# ~desc)
                    (throw e#))])
          ~@(emit-finally try))))

(defn add-read-handler! [conn open read-ch ^ServletInputStream in]
  (let [in-channel (Channels/newChannel in)
        buffer (ByteBuffer/allocate 1024)
        decoder (Decoder. (* 1024 64) frame-creator)
        abuffer (.array buffer)]
    (log/trace "Adding ReadListener")
    (.setReadListener
     in (reify ReadListener
          (onDataAvailable [_]
            (with-boundary "ReadListener.onDataAvailable"
              (while (and (:read @open)
                          (.isReady in))
                (let [cnt (with-boundary "ReadListener.onDataAvailable[read]"
                            (.read in abuffer))]
                  (.. buffer rewind (limit cnt))
                  (loop []
                    (when-let [frame (with-boundary "ReadListener.onDataAvailable[decode]"
                                     (.update decoder buffer))]
                      (.reset decoder)
                      (when (:read @open)
                        (>!! read-ch frame))
                      (recur)))))))
          (onAllDataRead [_]
            (with-boundary "ReadListener.onAllDataRead"
              (close! read-ch)
              (vswap! open disj :read)))
          (onError [this t]
            (log/error t "Error in websocket ReadListener")
            (.onAllDataRead this)
            (.close conn))))))

(defn add-write-handler! [conn open write-ch ^ServletOutputStream out]
  (log/trace "Adding WriteListener")
  (let [dout (DataOutputStream. out)]
    (.setWriteListener
     out (reify WriteListener
           (onWritePossible [this]
             (with-boundary "WriteListener.onWritePossible"
               (go-loop []
                 (if-let [frame (<! write-ch)]
                   (when (:write @open)
                     (with-boundary "WriteListener.onWritePossible[write]"
                       (encode frame dout))
                     (comment not thread safe?
                              thread (with-boundary "WriteListener.onWritePossible[flush]"
                                       (time (.flush out))))
                     (when (.isReady out)
                       (recur)))
                   (with-boundary "WriteListener.onWritePossible[close]"
                     (vswap! open disj :write)
                     (.close out))))))
           (onError [this t]
             (log/error t "Error in websocket WriteListener")
             (.close conn))))))

(def magic-cookie "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")

(defn- sha1-base64 [s]
  (let [md (MessageDigest/getInstance "SHA-1")]
    (.. Base64 getEncoder (encodeToString (.digest md (.getBytes ^String s))))))

(defn start-ws [web-conn on-init]
  (let [open (volatile! #{:read :write})
        read-chan (chan) write-chan (chan)]
    (log/debug "Initializing websocket connection" web-conn)
    (add-read-handler! web-conn open read-chan (.getInputStream web-conn))
    (add-write-handler! web-conn open write-chan (.getOutputStream web-conn))
    (on-init (rw-chan read-chan write-chan))
    (fn close []
      (log/debug "Destroying websocket connection" web-conn)
      (vreset! open #{})
      (close! read-chan)
      (close! write-chan))))

(defn accept-key [key]
  (sha1-base64 (str key magic-cookie)))

(defn start-async-ws [^HttpServletRequest request ^HttpServletResponse response on-init timeout]
  ;; FIXME honor timeout
  (binding [*init* #(start-ws % on-init)]
    (let [h (.upgrade request UpgradeHandler)]
      (.setStatus response 101)
      (.setHeader response "Upgrade" "websocket")
      (.setHeader response "Connection" "Upgrade")
      (.setHeader response "Sec-WebSocket-Accept"
                  (accept-key (let [key (.getHeader request "sec-websocket-key")]
                                (if (str/blank? key)
                                  (do (.setStatus response 400)
                                      (with-open [w (.getWriter response)]
                                        (.write w "No websocket key")))
                                  key)))))))
