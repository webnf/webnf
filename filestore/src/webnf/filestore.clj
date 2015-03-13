(ns ^{:doc "Blob store with content hashing.
We store files by their content hashes.
Storage is uncompressed to make zero-copy possible."}

  webnf.filestore
  (:import (java.io File InputStream Reader OutputStream PrintWriter OutputStreamWriter)
           (java.security MessageDigest DigestInputStream DigestOutputStream)
           javax.xml.bind.DatatypeConverter)
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.java.io :refer [file input-stream output-stream reader writer copy] :as io]
   [clojure.tools.logging :as log]))

(set! *warn-on-reflection* true)

(defn- sha-file ^java.io.File [{:keys [blob-dir]} ^String sha]
  (let [sha (.toLowerCase sha)
        dir (file blob-dir (subs sha 0 2))]
    (file dir (subs sha 2))))

;; ## Lowlevel API

(defn bytes->hex [^bytes b]
  (.toLowerCase (DatatypeConverter/printHexBinary b)))

(defn store-temp! [{:keys [store-dir secure-hash] :as store} f]
  (let [tf (File/createTempFile "temp-" ".part" store-dir)
        digest (MessageDigest/getInstance secure-hash)]
    (try
      (with-open [os (DigestOutputStream. (output-stream tf) digest)]
        (f os))
      ^{::store store}
      {:sha (.toLowerCase (DatatypeConverter/printHexBinary
                           (.digest digest)))
       :tempfile tf}
      (catch Exception e
        (log/error e "Error during file streaming. Deleting temp file.")
        (.delete tf)
        (throw e)))))

(defn merge-temp! [store {:keys [^File tempfile sha] :as tmp}]
  (assert (= store (::store (meta tmp)))
          "Temp file from different store")
  (let [dest (sha-file store sha)]
    (if (.isFile dest)
      (do (.delete tempfile)
          false)
      (let [p (.getParentFile dest)]
        (when-not p (throw (ex-info "Root dir" {:path dest})))
        (.mkdir p)
        (.renameTo tempfile dest)
        true))))

;; # Main API

(defn- compat-props [res]
  (select-keys res [:type :version :secure-hash]))

(defn- check-store-compat [res store-props]
  (let [want-props (compat-props res)]
    (when-not (= store-props want-props)
      (throw (ex-info "Store has a different format" {:want-format want-props
                                                      :has-format store-props})))))

(defn- write-store-props! [res prop-file]
  (with-open [o (writer prop-file)]
    (binding [*out* o]
      (pprint (into {} (compat-props res))))))

(defn make-store!
  "Constructor for a file store.
  Takes a path to a directory to store blobs in.
  Creates root if nessecary.
  :secure-hash can specify a hash-algorithm understood by java.security.MessageDigest"
  [root-path & {:keys [secure-hash]
                :or {secure-hash "SHA-1"}}]
  (let [sd (.getAbsoluteFile (file root-path))
        bd (file sd "blobs")
        pf (file sd "props")
        res {:type ::store-root
             :version [1]
             :store-dir sd
             :blob-dir bd
             :secure-hash secure-hash}]
    (assert sd)
    (.mkdirs bd)
    (if (.isFile pf)
      (check-store-compat res (read-string (slurp pf)))
      (write-store-props! res pf))
    res))

;; ## Writing

(defn stream-blob!
  "Calls back f with an java.io.OutputStream.
  Callback shall finish streaming to the blob before returning.

  Returns sha as a hex-encoded string."
  [store f]
  (let [{:keys [sha] :as res} (store-temp! store f)]
    (merge-temp! store res)
    sha))

(defn stream-copy-blob!
  "Copies input-stream until stream closes.

  Returns sha as a hex-encoded string."
  [store input-stream]
  (stream-blob! store #(copy input-stream %)))

(defn write-blob!
  "Call back f with a java.io.PrintWriter.
  Callback shall finish writing to the file before returning.

  Returns sha as a hex-encoded string."
  [store f]
  (stream-blob! store (fn [^OutputStream os]
                        (with-open [w (PrintWriter.
                                       (OutputStreamWriter. os))]
                          (f w)))))

;; ## Reading

(defn find-blob
  "Returns java.io.File by sha or nil."
  ^java.io.File [store sha]
  (let [f (sha-file store sha)]
    (when (.isFile f) f)))

(defn get-blob
  "Returns java.io.File by sha. Throws if not found."
  ^java.io.File [store sha]
  (if-let [f (find-blob store sha)]
    f
    (throw (ex-info (str "File not found: '" (sha-file store sha) "'"
                         {:store store :sha sha})))))

(defn open-stream
  "Returns java.io.InputStream by sha. Throws if not found."
  ^java.io.InputStream [store sha]
  (input-stream (get-blob store sha)))

(defn open-reader
  "Returns java.io.Reader by sha"
  ^java.io.Reader [store sha]
  (reader (open-stream store sha)))
