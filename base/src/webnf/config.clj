(ns webnf.config)

;; Allow configuration to be defined even before this namespace is loaded
(defonce configuration ::unbound)

(defn set-configuration! [cfg]
  (when (not= ::unbound configuration)
    (throw (ex-info "Runtime configuration already bound" {:var #'configuration
                                                           :cfg cfg})))
  (alter-var-root #'configuration (constantly cfg)))

(defn update-with-defaults!
  ([default-config] (update-with-defaults! default-config merge))
  ([default-config merge-fn]
   (alter-var-root
    #'configuration (fn [cur-config]
                      (if (= ::unbound cur-config)
                        default-config
                        (merge-fn default-config cur-config))))))
