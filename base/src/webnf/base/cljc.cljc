(ns webnf.base.cljc)

(defmacro defmacro* [name & doc+args+body]
  (let [[doc args & {:keys [clj cljs] :as body}]
        (if (string? (first doc+args+body))
          doc+args+body
          (cons "" doc+args+body))]
    `(defmacro ~name ~doc ~args
       (if (contains? ~'&env :context)
         ~(if (contains? body :cljs) cljs `(throw (ex-info "Not implemented in cljs" {:name name})))
         ~(if (contains? body :clj)  clj  `(throw (ex-info "Not implemented in clj" {:name name})))))))


