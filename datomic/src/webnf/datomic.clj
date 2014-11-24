(ns webnf.datomic
  "# Datomic connection helpers
   This namespace contains schema helpers, connection and transaction routines"
  (:import datomic.db.Db
           (java.util.concurrent BlockingQueue TimeUnit))
  (:require [clojure.core.async :as async :refer [go go-loop <! <!! >! >!! close! chan mult tap untap alt! put!]]
            [clojure.core.async.impl.protocols :refer [closed?]]
            [clojure.core.match :refer [match]]
            [clojure.core.typed :as typed :refer [ann Any All Keyword List defalias Map HMap HVec I U Seqable]]
            [clojure.repl]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [datomic.api :as dtm :refer [tempid]]
            [webnf.base :refer [forcat]]
            [webnf.kv :refer [treduce-kv assoc-when]]))

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
                  (symbol (namespace var-name) (name var-name))
                  (symbol (str (ns-name *ns*)) (name var-name)))
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
                                        :args (pr-str ~params)}
                                       e#))))))))

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

(defn dbfn-tx
  "Get transaction for installing a db function
   Example usage
   -------------
   (transact conn (dbfn-tx #'webnf.datomic/compose-tx))"
  [& fn-vars]
  (map (comp :dbfn/entity meta) fn-vars))


(defalias DbId (U datomic.db.DbId Long Keyword (HVec [Keyword Any])))
(defalias TxItem (U (HVec [Keyword Any *])
                    (I (Map Keyword Any)
                       (HMap :mandatory {:db/id DbId}))))
(defalias Tx (Seqable TxItem))

(ann compose-tx [Db (Seqable Tx) -> Tx])
(defn-db webnf.datomic/compose-tx
  "Composes multiple transactions, into one, as if they got transacted in order
   Example usage
   -------------
   [:webnf.datomic/compose-tx
    [
     [[:db/add (tempid :db.part/user) :db/ident :test/entity]]
     [[:db/add :test/entity :db/doc \"Cool\"]
      [:db/add (tempid :db.part/user) :db/lang :test/entity]]
    ]]"
  {:requires [[datomic.api :as d]]}
  [db* txs]
  (letfn [(tx-map-items [tm]
            (let [id (or (:db/id tm)
                         (throw (ex-info "Need :db/id" {:t tm})))]
              (for [[k v] (dissoc tm :db/id)]
                (if (.startsWith (name k) "_")
                  [:db/add v (keyword (namespace k) (subs (name k) 1)) id]
                  [:db/add id k v]))))
          (tx-tuple-items [db [op & args :as tt]]
            (case op (:db/add :db/retract) [tt]
                  (tx-items db (apply (:db/fn (d/entity db op))
                                      db args))))
          (tx-items [db tx]
            (mapcat #(cond (vector? %) (tx-tuple-items db %)
                           (map? %) (tx-map-items %)
                           :else (throw (ex-info "Not a tx element" {:t %})))
                    tx))
          (update-tx-ids [db tx-ids tempids]
            (reduce-kv (fn [res tid id]
                         (if (res id)
                           res
                           (assoc res id tid)))
                       tx-ids tempids))
          (update-id [db tempids tx-ids id]
            (let [tx-id (or (d/resolve-tempid db tempids id)
                            (d/entid db id)
                            (assert false (str "No id: " id)))]
              (get tx-ids tx-id tx-id)))
          (rewrite-tx [db tx tempids tx-ids]
            (for [[op e a v :as tx-item] (tx-items db tx)]
              (case op
                :db/add [op
                         (update-id db tempids tx-ids e)
                         a
                         (if (= :db.type/ref (:db/valueType (d/entity db a)))
                           (update-id db tempids tx-ids v)
                           v)]
                :db/retract tx-item)))
          (into-tx [tx-base tx]
            (persistent!
             (reduce (fn [res [_ e a :as item]]
                       (assoc! res [e a] item))
                     (transient tx-base) tx)))]
    (loop [txs' txs
           db' db*
           tx-ids {}
           tx' {}]
      (if-let [[tx & rst] (seq txs')]
        (let [{:keys [db-after tempids]} (d/with db' tx)
              tx-ids' (update-tx-ids db' tx-ids tempids)]
          (recur rst db-after tx-ids'
                 (into-tx tx' (rewrite-tx db' tx tempids tx-ids'))))
        (vals tx')))))

;; ## Connection routines

(defn connect
  "Create and connect to database. If created, transact schema.
   Return connection."
  ([db-uri]
     (dtm/create-database db-uri)
     (dtm/connect db-uri))
  ([db-uri schema & init-data]
     (let [created (dtm/create-database db-uri)
           conn (dtm/connect db-uri)]
       (when created
         (log/info "Creating schema on" db-uri)
         @(dtm/transact conn schema)
         (doseq [d init-data]
           @(dtm/transact conn d)))
       conn)))

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

;; ### Race - free report queue

(defn listen-reports
  "Connect to db and open a tx report queue.
   Listener channels can be added and removed by put!-ing tap and untap messages to ctl-chan:
   (put! ctl-chan [:tap listener-channel])
   (put! ctl-chan [:untap listener-channel])
   The first message on a freshly tapped listener channel will always be {:db most-current-db}
   Then, datomic transaction reports will arrive as they are received from the report queue."
  [db-uri ctl-chan]
  (let [conn (connect db-uri)
        ^BlockingQueue q (dtm/tx-report-queue conn)
        report-chan (chan)
        out-chan (chan)
        out-mult (mult out-chan)]
    ;; Set up loop to pull tx updates from the report queue and put them on report-chan
    (go (try (loop []
               (log/trace "TX Q step")
               (when-not (closed? ctl-chan)
                 (when-let [report (.poll q 10 TimeUnit/SECONDS)]
                   (log/trace "Received TX report from Q" report)
                   (>! report-chan report))
                 (recur)))
             (finally
               (log/trace "Closing TX Q")
               (dtm/remove-tx-report-queue conn)
               (close! report-chan))))
    ;; Loop to transfer from report-chan to the out mult,
    ;; + handling taps and untaps
    (go-loop [db (let [db (dtm/db conn)
                       {qdb :db-after} (.poll q)]
                   (if (and qdb (> (dtm/basis-t qdb)
                                   (dtm/basis-t db)))
                     qdb db))]
      (log/trace "Delivery step")
      (alt! report-chan ([{:as report :keys [db-after]}]
                         (cond (nil? report)
                               (do (log/error "Report chan closed, closing control-chan")
                                   (close! ctl-chan)
                                   {:error :report-chan-died})

                               (> (dtm/basis-t db-after)
                                  (dtm/basis-t db))
                               (do (log/trace "Delivering TX report" report)
                                   (>! out-chan report)
                                   (recur db-after))

                               :else (recur db)))
            ctl-chan ([ctl-msg]
                      (log/trace "Receiving CTL message" ctl-msg)
                      (match [ctl-msg]
                             [nil] (do (close! out-chan)
                                       {:success :shutdown})
                             [{:control :tap :channel ch}] (do (put! ch {:db db})
                                                               (tap out-mult ch)
                                                               (recur db))
                             [{:control :untap :channel ch}] (do (untap out-mult ch)
                                                                 (recur db))
                             :else (do (log/error "Unknown control message" ctl-msg)
                                       (log/info "Resuming control loop")
                                       (recur db))))))))
