(ns webnf.datomic
  "# Datomic connection helpers
   This namespace contains schema helpers, connection and transaction routines"
  (:import datomic.db.Db)
  (:require
   [webnf.base :refer [forcat]]
   [webnf.kv :refer [treduce-kv assoc-when]]
   [clojure.core.match :refer [match]]
   [clojure.core.async :as async :refer [go <! <!! >! >!! close!]]
   [clojure.tools.logging :as log]
   [datomic.api :as dtm :refer [tempid]]
   [clojure.core.typed :as typed :refer [ann All Keyword]]
   [clojure.algo.monads :refer [state-m set-state fetch-state domonad defmonadfn
                                with-state-field m-fmap]]
   [clojure.repl]))

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
             :db/valueType (if (namespace type)
                             type
                             (keyword "db.type" (name type)))
             :db/cardinality (if (:many flags)
                               :db.cardinality/many
                               :db.cardinality/one)}
            doc (assoc :db/doc doc)
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

(defn- parse-fn-body [name args]
  (let [[typ args] (if (= :- (first args))
                     [(second args) (nnext args)]
                     [nil args])
        [doc args] (if (string? (first args))
                     [(first args) (next args)]
                     [nil args])
        [flags [params & body]] (if (map? (first args))
                                  [(first args) (next args)]
                                  [nil args])
        known-flags #{:imports :requires :part :db-requires}
        _ (assert (every? known-flags (keys flags)) (str "Flags can be of " known-flags))
        [params' lets] (reduce (fn [[params lets] param]
                                 (if (symbol? param)
                                   [(conj params param) lets]
                                   (let [s (gensym "param-")]
                                     [(conj params s) (into lets [param s])])))
                               [[] []] params)
        {:keys [imports requires part db-requires] :or {part :db.part/user}} flags]
    {:name name :doc doc :params params' :imports imports :requires requires :db-requires db-requires
     :part part :lets lets :body body :typ typ}))

(defn- resolve-db-require [r]
  (let [[local-name var-name] (match [r]
                                     [[var-name]] [nil var-name]
                                     [[local-name var-name]] [local-name var-name])
        var-sym (if (namespace var-name)
                  var-name
                  (symbol (str (ns-name *ns*)) (str var-name)))
        the-var (find-var var-sym)
        varm (meta the-var)]
    (assert varm (str "No var " var-sym))
    [(or local-name (symbol (name var-name)))
     (:dbfn/name varm)
     (:dbfn/type varm)]))

(ann extract-fn (All [f] [Db Keyword -> f]))
(defn ^:no-check extract-fn [db fname]
  (:db/fn (datomic.api/entity db fname)))

(defn- dbfn-source [{:keys [name doc params body imports requires part db-requires lets]} version]
  (let [db-sym (first params)]
    `(let ~(into lets (forcat [r db-requires]
                              (let [[local-name fn-name typ] (resolve-db-require r)]
                                [local-name
                                 (if (and typ (= version :local))
                                   `(clojure.core.typed/ann-form (extract-fn ~db-sym ~fn-name) ~typ)
                                   `(:db/fn (datomic.api/entity ~db-sym ~fn-name)))])))
       (try ~@body (catch Exception e#
                     (if (instance? clojure.lang.ExceptionInfo e#)
                       (throw (ex-info (.getMessage e#)
                                       (assoc (ex-data e#)
                                         :name ~(pr-str name)
                                         :args (pr-str ~params))))
                       (throw (ex-info "Wrapped native exception" 
                                       {:name ~(pr-str name)
                                        :args (pr-str ~params)
                                        :wrapped e#
                                        :cause (let [w# (java.io.StringWriter.)]
                                                 (binding [*err* w#]
                                                   (clojure.repl/pst e#))
                                                 (str w#))}))))))))

(defn- make-db-fn [{:keys [name doc params body imports requires part db-requires lets] :as desc}]
  `(assoc-when {:db/id (tempid ~part)
                :db/ident ~(keyword name)
                :db/fn (dtm/function (assoc-when {:lang :clojure
                                     :params '~params
                                     :code '~(dbfn-source desc :transactor)}
                                    :imports '~imports
                                    :requires (into '~requires
                                                    '[[clojure.repl]
                                                      [clojure.pprint]])))}
               :db/doc ~doc))

(defmacro function
  "Define a database function to be transacted. Syntax similar to clojure.core/fn, attributes can hold:
   :part the db partition for the tempid
   :imports for the db function
   :requires for the db function
   :db-requires generates a wrapping let that binds another database function (assumes db as first parameter)"
  {:arglists (list '[name doc? [& params] & body]
                   '[name doc? attributes [& params] & body])}
  [name & args]
  (make-db-fn (parse-fn-body name args)))

(defmacro defn-db
  "Defines a database function in regular clojure, for it to be accessible to type checking, direct calling, ...
   Has the :db/fn entity in :dbfn/entity of var metadata."
  {:arglists (list '[name doc? [& params] & body]
                   '[name doc? {:imports [& imports] :requires [& requires] :part db-part} [& params] & body])}
  [fname & args]
  (let [{:keys [doc params requires typ imports] :as desc} (parse-fn-body fname args)
        fnsym (symbol (name fname))]
    `(do ~@(when (seq requires) [`(require ~@(map (partial list 'quote) requires))])
         ~@(when (seq imports) [`(import ~@imports)])
         ~@(when typ [`(ann ~fnsym ~typ)])
         (defn ~(with-meta fnsym
                  (assoc (meta fname) :dbfn/name (keyword fname)
                         :dbfn/entity (make-db-fn desc)
                         :dbfn/type (list 'quote typ)))
           ~@(when doc [doc]) ~params
           ~(dbfn-source desc :local)))))

(defmacro defm-db
  "Defines a monad to be run in the current-db state monad"
  [mname steps expr]
  `(def ~mname (domonad state-m ~steps ~expr)))

(defn m-transact [tx']
  (fn [{:keys [db tx ti]}]
    (let [{:keys [db-after tempids]} (dtm/with db tx')]
      (assert (distinct? (keys ti) (keys tempids))
              "Clashing tempids")
      [db-after
       {:db db-after
        :ti (into (or ti {}) tempids)
        :tx (into (or tx []) tx')}])))

(def m-db (juxt identity :db))

(defn m-resolve-tempid [tid]
  (fn [{:keys [db tempids] :as st}]
    [(dtm/resolve-tempid db tempids tid) st]))

(defmonadfn m-entity 
  ([eid k] (m-fmap k (m-entity eid)))
  ([eid]
     (fn [{db :db :as st}]
       [(dtm/entity db eid) st])))

(defmonadfn m-fn [eid]
  (m-entity eid :db/fn))

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
