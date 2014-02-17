(ns webnf.middleware.browser-http
  (:import (java.io ByteArrayOutputStream StringReader ByteArrayInputStream))
  (:require
   [ring.util.response :as r]
   [clojure.java.io :as io]
   [webnf.kv :refer [merge-deep]]
   [clojure.string :as str]
   [webnf.utils :refer [to-many]]))

(defn kw-methods [meth]
  {:pre [(string? meth)]}
  (let [meth (.toUpperCase meth)]
    (get {"GET" :get
          "POST" :post
          "PUT" :put
          "DELETE" :delete}
         meth)))

(defn replacement-body [req body]
  (let [bos (ByteArrayOutputStream. (int (* 1.2 (count body))))]
    (io/copy (StringReader. body) bos
             :encoding (:character-encoding req "UTF-8"))
    (let [buf (.toByteArray bos)]
      {:headers {"content-length" (str (count buf))}
       :body (ByteArrayInputStream. buf)})))

(defn add-headers [{h :headers :as req} headers]
  (let [hmap (into {} (map #(str/split % #":")
                           (to-many headers)))]
    (assoc req :headers (merge h hmap))))

(defn wrap-browser-http
  ([handler] (wrap-browser-http handler (constantly true)))
  ([handler adapt-request?] 
     (fn [req]
       (if (adapt-request? req)
         (let [{{meth "_http_method"
                 body "_http_body"
                 head "_http_header"} :params}
               req
               
               {status :status :as resp} 
               (handler (cond-> req
                                meth (assoc :request-method (kw-methods meth))
                                body (merge-deep (replacement-body req body))
                                head (add-headers head)))]
           (cond-> resp
                   (= 201 status) (r/status 303)
                   (= 204 status) (-> (r/status 303)
                                      (r/header "Location" (:uri req)))))
         (handler req)))))
