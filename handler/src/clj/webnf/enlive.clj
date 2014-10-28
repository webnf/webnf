(ns webnf.enlive
  (:import java.net.URLDecoder)
  (:require
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [clojure.core.cache :as cache]
   [net.cgrand.enlive-html :refer [set-attr clone-for html-resource at emit* content substitute select]]))

(defprotocol HtmlSource
  (to-html [source])
  (cache-key [source]))

(extend-protocol HtmlSource
  clojure.lang.IFn
  (to-html [f] (f))
  (cache-key [_] nil)
  
  String
  (to-html [s]   (when-let [r (io/resource s)]
                   (to-html r)))
  (cache-key [s] (when-let [r (io/resource s)]
                   (cache-key r)))

  java.net.URL
  (to-html [url] (html-resource url))
  (cache-key [url]
    (if (= "file" (.getProtocol url))
      (cache-key (io/file (URLDecoder/decode (.getPath url) "UTF-8")))
      url))

  java.io.File
  (to-html [f] (html-resource f))
  (cache-key [f]
    [(.lastModified f) (.getAbsolutePath f)]))

(defonce cache (atom (cache/lru-cache-factory {})))

(defn load-html [src]
  (if-let [key (cache-key src)]
    (or (cache/lookup @cache key)
        (let [data (to-html src)]
          (log/debug "Loading template data" src)
          (swap! cache cache/miss key data)
          data))
    (to-html src)))

(defmacro deftemplate [name source params & rules]
  `(let [src# ~source]
     (defn ~name ~params
       (emit*
        (at (load-html src#)
            ~@rules)))))

(defmacro defsnippet [name source selector params & rules]
  `(let [src# ~source
         sel# ~selector
         snip-src# (reify
                     Object
                     (toString [_] ~(str source " " (pr-str selector)))
                     HtmlSource
                     (to-html [_]
                       (select (to-html src#) sel#))
                     (cache-key [_]
                       [(cache-key src#) sel#]))]
     (defn ~name ~params
       (at (load-html snip-src#)
           ~@rules))))
