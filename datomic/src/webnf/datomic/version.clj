(ns webnf.datomic.version
  (:require [clojure.string :as str]
            [webnf.datomic :refer [enum field]]
            [datomic.api :as dtm :refer [tempid]]
            [clojure.tools.logging :as log]))

;; Utils for simple dotted versions

(defn- str->version [s]
  (map #(Long/parseLong %) (str/split (str s) #"\.")))

(defn compare-versions [v1 v2]
  (loop [[s1 & v1'] (str->version v1)
         [s2 & v2'] (str->version v2)]
    (cond
     (and (nil? s1) (nil? s2)) 0
     (nil? s2) 1
     (nil? s1) -1
     :else (let [c (compare s1 s2)]
             (if (zero? c)
               (recur v1' v2')
               c)))))

(defn version= [v1 v2]
  (zero? (compare-versions v1 v2)))

(defn version< [v1 v2]
  (neg? (compare-versions v1 v2)))

(defn version<= [v1 v2]
  (not (pos? (compare-versions v1 v2))))

(defn major= [v1 v2]
  (let [[mj1 mi1] (str->version v1)
        [mj2 mi2] (str->version v2)]
    (= mj1 mj2)))

(defn minor= [v1 v2]
  (let [[mj1 mi1] (str->version v1)
        [mj2 mi2] (str->version v2)]
    (and (= mj1 mj2)
         (= mi1 mi2))))

(def version-schema
  [(enum "Version schema marker" :webnf.datomic.schema/schema)
   (field "Version of db schema" :webnf.datomic.schema/version :string)
   (field "Dependency schemata of db schmea" :webnf.datomic.schema/dependencies :ref :many :component)
   (field "Ident of dependency schema" :webnf.datomic.schema.dep/ident :keyword)])

(defn- ensure-schema-schema! [conn]
  (when-not (dtm/entity (dtm/db conn) :webnf.datomic.schema/schema)
    (log/info "Transacting schema schema")
    @(dtm/transact conn version-schema)))

(defn ensure-schema! [conn ident version schema-tx]
  (ensure-schema-schema! conn)
  (let [db (dtm/db conn)
        {:as version-info
         version' :webnf.datomic.schema/version
         dependencies :webnf.datomic.schema/dependencies}
        (dtm/entity db ident)]
    (log/debug "Ensuring" ident version "against" version-info)
    (cond
     (and schema-tx (not version-info))
     (do (log/info "Creating schema" ident "with version" version)
         @(dtm/transact conn schema-tx)
         (recur conn ident version schema-tx))
     (version= version version')
     ;; recursively ensure dependencies
     (doseq [{:keys [webnf.datomic.schema.dep/ident
                     webnf.datomic.schema/version]}
             dependencies]
       (log/debug "Checking dep" ident version)
       (ensure-schema! conn ident version nil))
     (and schema-tx (major= version version'))
     (do (log/info "Auto upgrading schema" ident "from version" version' "to" version)
         @(dtm/transact conn (cons [:db.fn/retractEntity (:db/id version-info)]
                                   schema-tx))
         (recur conn ident version schema-tx))
     :else (throw (ex-info (str "Version of [" ident " " version' "] doesn't match expected version " version)
                           {:schema ident :has-version version' :expect-version version})))))

(defn version-tx [ident version deps]
  (let [vid (tempid :db.part/user)]
    (cons {:db/id vid
           :db/ident ident
           :webnf.datomic.schema/version version}
          (for [[ident version] deps]
            {:db/id (tempid :db.part/user)
             :webnf.datomic.schema/_dependencies vid
             :webnf.datomic.schema.dep/ident ident
             :webnf.datomic.schema/version version}))))
