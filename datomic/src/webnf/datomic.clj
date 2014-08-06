(ns webnf.datomic
  "# Datomic connection helpers
   This namespace contains schema helpers, connection and transaction routines"
  (:require
   [webnf.kv :refer [treduce-kv assoc-when]]
   [clojure.core.async :as async :refer [go <! <!! >! >!! close!]]
   [clojure.tools.logging :as log]
   [datomic.api :as dtm :refer [tempid]]))

;; ## Schema generation helpers

(defn field
  "Shortcut syntax for defining datomic schema fields.

   Parameters:
   - doc, an optional docstring
   - id, a namespaced keyword for the field id
   - type, a keyword getting auto-namespaced to db.type, if it hasn't a namespace
   - flags, zero or more of #{:many :index :unique :identity :fulltext :no-history :component}

   Example:
   (field \"Optional docstring\" :schema.field/id :string :index)"
  {:arglists '([doc id type & flags] [id type & flags])}
  [& args]
  (let [[doc id type & flags] (if (string? (first args))
                                args
                                (cons nil args))
        flags (into #{} flags)]
    (cond-> {:db/id (tempid :db.part/db)
             :db.install/_attribute :db.part/db
             :db/ident id
             :db/doc doc
             :db/valueType (if (namespace type)
                             type
                             (keyword "db.type" (name type)))
             :db/cardinality (if (:many flags)
                               :db.cardinality/many
                               :db.cardinality/one)}
            (:index flags) (assoc :db/index true)
            (:unique flags) (assoc :db/unique
                              (if (:identity flags)
                                :db.unique/identity
                                :db.unique/value))
            (:fulltext flags) (assoc :db/fulltext true)
            (:component flags) (assoc :db/isComponent true)
            (:no-history flags) (assoc :db/noHistory true))))

(defn enum
  "Shortcut syntax for an enum value schema field"
  {:arglists '([doc id] [id])}
  [& args]
  (let [[doc id] (if (string? (first args))
                   args (cons nil args))]
    (assoc-when {:db/id (tempid :db.part/user)
                 :db/ident id}
                :db/doc doc)))

;; # TODO allow for destructoring in param list

(defmacro function
  "Define a database function to be transacted. Syntax similar to clojure.core/fn, attributes can hold:
   :part the db partition for the tempid
   :imports for the db function
   :requires for the db function"
  {:arglists (list '[name doc? [& params] & body]
                   '[name doc? {:imports [& imports] :requires [& requires] :part db-part} [& params] & body])}
  [name & args]
  (let [[doc args] (if (string? (first args))
                     [(first args) (next args)]
                     [nil args])
        [flags [params & body]] (if (map? (first args))
                                  [(first args) (next args)]
                                  [nil args])
        {:keys [imports requires part] :or {part :db.part/user}} flags
        known-flags #{:imports :requires :part}
        _ (assert (every? known-flags (keys flags)) (str "Flags can be of " known-flags))
        body* `(try ~@body (catch Exception e#
                             (if (instance? clojure.lang.ExceptionInfo e#)
                               (throw e#)
                               (throw (ex-info "Wrapped native exception" 
                                               {:cause
                                                (let [w# (java.io.StringWriter.)]
                                                  (binding [*err* w#]
                                                    (clojure.repl/pst e#))
                                                  (str w#))})))))]
    `(assoc-when {:db/id (tempid ~part)
                  :db/ident ~(keyword name)
                  :db/fn (dtm/function (assoc-when {:lang :clojure
                                                    :params '~params
                                                    :code ~(pr-str body*)}
                                                   :imports '~imports
                                                   :requires (into '~requires
                                                                   '[[clojure.repl]
                                                                     [clojure.pprint]])))}
                 :db/doc ~doc)))

;; ## Connection routines

(defn connect
  "Create and connect to database. If created, transact schema.
   Return connection."
  [db-uri schema & init-data]
  (let [created (dtm/create-database db-uri)
        conn (dtm/connect db-uri)]
    (when created
      (log/info "Creating schema on" db-uri)
      @(dtm/transact conn schema)
      (doseq [d init-data]
        @(dtm/transact conn d)))
    conn))

(defn recreate!!
  "Drop database and recreate, returning connection."
  [db-uri schema & init-data]
  (dtm/delete-database db-uri)
  (apply connect db-uri schema init-data))

;; ### core.async base transaction executor
;; - `tx-executor` executes and waits on transactions, then putting the result into returned channel.
;; - `accumulate-tx-results` is an example implementation of what to do with result channel
;;   with reasonable default behavior

(defn tx-executor
  "Take transactions from channel and execute them on conn. Returns an output channel that yields:
   {:tx transaction :result dereferenced-tx} or {:error exception}
   Up to concurrent-tx transactions are executed in parallel."
  ([conn tx-in-channel]
     (tx-executor conn tx-in-channel 8))
  ([conn tx-in-channel concurrent-tx]
     (let [running-tx (async/pipe (async/map< 
                                   #(try {:tx (dtm/transact conn %)}
                                         (catch Exception e
                                           {:error e}))
                                   tx-in-channel)
                                  (async/chan concurrent-tx))]
       (async/map< (fn [{:keys [tx error] :as in}]
                     (if error 
                       in 
                       (try (assoc in :result @tx)
                            (catch Exception e
                              (assoc in :error e)))))
                   running-tx))))

(defn accumulate-tx-results
  "A simple sink for the return channel of tx-executor.
   Returns channel, that delivers a single, reduced result, once input channel closes:
   {:success success-count :errors [list of exceptions]}"
  [tx-result-channel]
  (async/reduce (fn [out {:keys [error rx result] :as in}]
                  (if error
                    (do (log/error error "Failed transaction")
                        (update-in out [:errors] conj in))
                    (do (log/debug "Finishing transaction" result)
                        (update-in out [:success] inc))))
                {:success 0 :errors []}
                tx-result-channel))

;; ### Middleware for proper request database access

(defn wrap-connection
  "Ring Middleware connecting to a database on creation, gets database at begin of every request.
   Associates :datomic/conn and :datomic/db to the request."
  [h uri]
  (fn [req]
    (let [conn (dtm/connect uri)]
      (h (assoc req
           :datomic/conn conn
           :datomic/db (dtm/db conn))))))
