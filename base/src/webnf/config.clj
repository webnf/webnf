(ns webnf.config
  "Container configurations

  Facilities to allow applications to define default configurations,
  overridable by containers.  Configuration can live directly in
  webnf.config/configuration, or in a subkey. Either way, functions in
  this namespace ensure there will not be silent shadowing."
  (:require [clojure.set :as set]))

;; Allow configuration to be defined even before this namespace is loaded
(defonce ^:redef configuration ::unbound)

(defn- get-configuration*
  "Map lookup (get-in) in the maybe monad (::unbound == Nothing)"
  [cfg keys]
  (cond
    (= ::unbound cfg) ::unbound
    (seq keys)        (recur (get cfg (first keys) ::unbound)
                             (next keys))
    :else             cfg))

(defn get-configuration
  "Get configuration for sub keys"
  ([] configuration)
  ([keys]
   (get-configuration* configuration keys)))

;; Map merge in the maybe monad

(declare merge-config)

(defn- merge-config-keys [left right]
  (persistent!
   (reduce #(assoc! %1 %2 (merge-config
                           (get left  %2 ::unbound)
                           (get right %2 ::unbound)))
           (transient {}) (into #{} (concat (keys left)
                                            (keys right))))))

(defn- merge-config [left right]
  (cond
    (= ::unbound left)                          right
    (= ::unbound right)                         left
    (and (map? left) (map? right))              (merge-config-keys left right)
    (and (not (map? left)) (not (map? right)))  right
    :else                                       (throw (ex-info "Incompatible default config" {:left left :right right}))))

(defn- cfg-map [keys sub-cfg]
  (if-let [[k & ks] (seq keys)]
    {k (cfg-map ks sub-cfg)}
    sub-cfg))

(defn set-configuration!
  ([cfg] (set-configuration! nil cfg))
  ([keys cfg]
   (alter-var-root
    #'configuration
    (fn [cfg-root]
      (let [cfg* (get-configuration* cfg-root keys)]
        (if (= ::unbound cfg*)
          (merge-config
           cfg-root (cfg-map keys cfg))
          (throw (ex-info "Runtime configuration already bound" {:var #'configuration
                                                                 :keys keys
                                                                 :cfg cfg*}))))))))

(defn update-with-defaults!
  ([default-config] (update-with-defaults! nil default-config))
  ([keys default-config]
   (alter-var-root
    #'configuration
    #(merge-config (cfg-map keys default-config) %))))
