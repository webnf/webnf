# webnf/filestore

Stores files content-hashed a'la git, but stores file contents unmodified,
so that you can `sendfile()` to a socket or otherwise directly use file contents
(git adds a binary header, to distinguish trees, commits and blobs and also zlib compresses).

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/webnf/filestore.svg)](https://clojars.org/webnf/filestore)

There are a couple of helper functions, but this is the basic API.

```clj
(require '[webnf/filestore :as fs])

(defonce store (fs/make-store! "/var/db/blobstore"))

;; (java.io.OutputStream -> _|_) -> java.lang.String, hex encoded
(def sha (fs/stream-blob! store (fn [os] (.write os (.getBytes "File Content" "UTF-8")))))
;; java.lang.String, hex-encoded -> java.io.File, or nil when not found
(def file (fs/find-blob store sha))
```
