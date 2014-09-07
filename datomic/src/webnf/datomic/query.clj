(ns webnf.datomic.query
  "# Query functions
   Build an manipulate queries and handle query results"
  (:require
   [webnf.kv :refer [treduce-kv]]
   [datomic.api :refer [q tempid entity]]))

(defn ?qsym "Gensym query symbols"
  ([] (?qsym ""))
  ([prefix] (symbol (str "?" (gensym prefix)))))

;; ## Building queries
;; Use add-in to add to queries started with by-attr, by-value ...

;; The by-* functions take an optional first symbol argument, used as
;; a query symbol for the entity id. If not passed, is generated with ?qsym

(defn add-in "Append to key in query map, keys e.g. :find :in :where"
  [q key & vals]
  (update-in q [key] into vals))

(defn by-attr "Query entities with attribute"
  {:arglists '([id-sym attr & clauses] [attr & clauses])}
  [& args]
  (let [[id-sym attr & clauses] (if (symbol? (first args))
                                  args (cons (?qsym "id") args))]
    {:find [id-sym]
     :where (into [[id-sym attr]] clauses)}))

(defn by-value "Query entities where attribute is of value"
  {:arglists '([id-sym attr value & clauses] [attr value & clauses])}
  [& args]
  (let [[id-sym attr value & clauses] (if (symbol? (first args))
                                        args (cons (?qsym "id") args))]
    {:find [id-sym]
     :where [(cond-> [id-sym attr]
                     value (conj value))]}))

;; ## Id result functions

;; These help with getting query result data into the right shape

(defn reify-entity
  "Pour datomic entity into a map, with :db/id included."
  [{id :db/id :as e}]
  (treduce-kv assoc! {:db/id id} e))

;; These handle result tuple sets, where the first element is an entity id
;; (single database id, datomic entity and lists of these).
;; The *-1 variants return single results and ensure that query returned 0 or 1 result.

(defn id-1
  "Wrapper around q, returns first element, asserts single result tuple"
  [query db & sources]
  (let [res (apply q query db sources)]
    (assert (>= 1 (count res)) "Multiple return values for entity-1")
    (ffirst res)))

(defn entity-1
  "Wrapper around q to return a single entity from the ffirst result.
   Ensures a single result tuple."
  [query db & sources]
  (when-let [db-id (apply id-1 query db sources)]
    (entity db db-id)))

(defn id-list
  "Wrapper around q to return a list of the first element of every result tuple"
  [query db & sources]
  (map first (apply q query db sources)))

(defn entity-list
  "Wrapper around q to return a list of entities from the first element 
   (as an entity id) of every result tuple."
  [query db & sources]
  (map (fn [[id]] (entity db id))
       (apply q query db sources)))
