(ns webnf.async-servlet.impl
  (:import
   (java.io InputStream File FileInputStream OutputStream)
   (javax.servlet AsyncListener AsyncContext AsyncEvent ServletConfig)
   (javax.servlet.http HttpServlet HttpServletRequest HttpServletResponse))
  (:require [ring.util.servlet :as servlet]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(defn resolve-config-var [^ServletConfig config ^String name]
  (when-let [vname (.getInitParameter config name)]
    (let [vsym (symbol vname)
          ns (symbol (namespace vsym))]
      (require ns)
      (resolve vsym))))

(defmacro with-flush [bindings & body]
  (assert (vector? bindings) "a vector for its binding")
  (assert (even? (count bindings))) "an even number of forms in binding vector"
  (cond
   (= (count bindings) 0) `(do ~@body)
   (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                             (try
                               (with-flush ~(subvec bindings 2) ~@body)
                               (finally
                                 (. ~(bindings 0) flush))))
   :else (throw (IllegalArgumentException.
                 "with-flush only allows Symbols in bindings"))))

(defn set-body
  "Update a HttpServletResponse body with a String, ISeq, File or InputStream."
  [^HttpServletResponse response, body]
  (cond
   (string? body)
   (with-open [writer (.getWriter response)]
     (.print writer body))
   (seq? body)
   (with-open [writer (.getWriter response)]
     (doseq [chunk body]
       ;; removed .flush, should lead to better performance
       (.print writer (str chunk))))
   (instance? InputStream body)
   (with-open [^InputStream b body
               ^OutputStream os (.getOutputStream response)]
     (io/copy b os))
   (instance? File body)
   (let [^File f body]
     (with-open [stream (FileInputStream. f)]
       (set-body response stream)))
   (nil? body)
   nil
   :else
   (throw (Exception. ^String (format "Unrecognized body: %s" body)))))

(defn write-chunk
  "Update a HttpServletResponse body with a String, ISeq, File or InputStream."
  [^HttpServletResponse response, body]
  (cond
   (string? body)
   (with-flush [writer (.getWriter response)]
     (.print writer body))
   (seq? body)
   (with-flush [writer (.getWriter response)]
     (doseq [chunk body]
       (.print writer (str chunk))))
   (instance? InputStream body)
   (with-open [^InputStream b body]
     (with-flush [^OutputStream os (.getOutputStream response)]
       (io/copy b os)))
   (instance? File body)
   (with-open [stream (FileInputStream. ^File body)]
     (write-chunk response stream))
   (nil? body)
   nil
   :else
   (throw (Exception. ^String (format "Unrecognized body: %s" body)))))

(defn make-listener [{:keys [error timeout complete]}]
  (reify AsyncListener
    (onStartAsync [this event]
      ;; we won't react to this, since
      ;; 1. it's name is confusing
      ;; 2. reusing context between requests
      ;;    doesn't fit into ring's request-response model
      nil)
    (onError [this event]
      (when error
        (error event)))
    (onTimeout [this event]
      (when timeout
        (timeout event)))
    (onComplete [this event]
      (when complete
        (complete event)))))

(defn start-async [^HttpServletRequest request make-callbacks timeout]
  (let [ctx (.startAsync request)]
    (when timeout
      (.setTimeout ctx timeout))
    (log/tracef "Started async response with timeout: %.1fs"
                (double (/ (or timeout (.getTimeout ctx)) 1000)))
    (.addListener ctx (make-listener (make-callbacks ctx)))))

(defn handle-servlet-request 
  ([handler ^HttpServletRequest request response]
     (let [request-map (assoc (servlet/build-request-map request)
                         :path-info (.getPathInfo request))
           {:keys [status headers body timeout] :as response-map} (handler request-map)]
       (when status
         (servlet/set-status response status))
       (when-not (empty? headers)
         (servlet/set-headers response headers))
       (if (fn? body)
         (start-async request body timeout)
         (set-body response body)))))



