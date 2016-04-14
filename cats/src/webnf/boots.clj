(ns webnf.boots
  (:require
   [clojure.tools.logging :as log]
   [clojure.test :refer :all]
   (webnf.cats
    [katie :refer [pure *> <$> <> <*>]]
    [connie :refer [cont* defcontinuation* defcontinuation continue* run-cont]]
    [monie :refer [>>= mdo]]))
  (:import (java.nio CharBuffer)))

;; ## State monad
;; Monad (fn [& st])

(defn state-t [m]
  (cont* k (fn [& st] (continue*
                       m (fn [& vals]
                           (apply (apply k vals) st))))))

(defn run-state*
  ([state-m init-state] (run-state* state-m init-state identity))
  ([state-m init-state final-f]
   (apply (continue*
           state-m (fn [& vals]
                     (fn [& final-state]
                       (log/debug :final-state final-state)
                       (apply final-f vals))))
          init-state)))

(defn run-state [state-m & init-state]
  (run-state* state-m init-state vector))

(def state-get
  (cont* k (fn [& st]
             (apply (apply k st) st))))

(defn state-set [& new-st]
  (cont* k (fn [& st]
             (apply (apply k st) new-st))))

(deftest state-m
  (is (= :init (run-state* state-get [:init])))
  (is (= [:tini] (run-state (*> state-get
                                (pure :tini))
                            :init))))

(run-state (mdo [[answer] (state-t (pure 42))
                 [& st] state-get]
                (pure answer st))
           :foo :bar)

(defcontinuation Left left ^{:self-as self} [k v]
  self)

(defcontinuation Right right [k v]
  (k v))

(continue* (>>= (right 0) (comp pure inc)) right)
(run-cont (<$> inc (left 0)) right)

(run-cont
 (run-state* (mdo [[answer] (state-t (right 42))
                   [& st] state-get]
                  (pure (pure [answer st])))
             [:st1])
 right)

;; Parser

(defprotocol Parser
  (p-read  [p on-fail on-partial start end position source]
    "Attempt to read at sorce position")
  (p-write [p on-fail on-partial start end position sink values]
    "Attempt to write at sink position"))

(defn run-char-parser [parser char-buffer]
  (letfn [(on-fail [p k start end position]
            {:state :fail :parser p :source char-buffer
             :k k :start start :end end :position position})
          (on-partial [p k start end position checkpoint]
            {:state :partial :parser p :source char-buffer
             :k k :start start :end end :position position
             :checkpoint checkpoint})]
    (let [start (.position char-buffer)
          end (.limit char-buffer)]
      (p-read (continue*
               parser (fn [& vals]
                        (reify Parser
                          (p-read [_ on-fail* on-partial* start* end* position* source*]
                            {:state :success
                             :leftover (.subSequence char-buffer position* end)
                             :result vals}))))
              on-fail on-partial start end start char-buffer))))

(defn run-char-printer [parser char-buffer values]
  (letfn [(on-fail [p k start end position values & error]
            {:state :fail :parser p :sink char-buffer
             :k k :start start :end end :position position
             :error error :values values})
          (on-partial [p k start end position values checkpoint]
            {:state :partial :parser p :sink char-buffer
             :k k :start start :end end :position position
             :values values :checkpoint checkpoint})]
    (let [start (.position char-buffer)
          end (.limit char-buffer)]
      (p-write (continue* parser
                          (fn [& vals]
                            (reify Parser
                              (p-write [p on-fail* on-partial* start* end* position* sink* values*]
                                (if (seq values*)
                                  {:state :partial :parser p :sink char-buffer
                                   :position position* :leftover values*}
                                  {:state :success
                                   :sink char-buffer
                                   :position position*})))))
               on-fail on-partial start end start char-buffer values))))

(def ch
  (cont*
   k
   (reify
     Parser
     (p-read  [p on-fail on-partial start end position source]
       (assert (>= position start))
       (cond
         (>= position end) (on-partial p k start end position nil)
         :else (p-read (k (.get ^CharBuffer source (int position)))
                       on-fail on-partial start end (inc position) source)))
     (p-write [p on-fail on-partial start end position sink [fv & nv :as values]]
       (assert (>= position start))
       (cond
         (not (seq values)) (on-fail p k start end position values "No char present")
         (not (char? fv)) (on-fail p k start end position values
                                   "Not a character: " (pr-str fv))
         (>= position end) (on-partial p k start end position values nil)
         :else (do
                 (.put ^CharBuffer sink (int position) (char fv))
                 (p-write (k fv)
                          on-fail on-partial start end (inc position) sink nv)))))))

(defn item [parser join & [split]]
  (cont*
   k (reify
       Parser
       (p-read [p on-fail on-partial start end position source]
         (p-read (continue* parser (comp k join))
                 on-fail on-partial start end position source))
       (p-write [p on-fail on-partial start end position sink [fv & nv :as values]]
         (p-write (continue* parser (fn [& pieces]
                                      #_(assert (= fv (apply join pieces)) (str (pr-str fv) " != "
                                                                                (pr-str (apply join pieces))))
                                      (reify Parser
                                        (p-write [_ on-fail on-partial start end position sink values]
                                          (if (= nv values)
                                            (p-write (k fv)
                                                     on-fail on-partial start end position sink values)
                                            (on-fail p k start end position values
                                                     "Item didn't fully emit"
                                                     (str (pr-str fv) " != "
                                                          (pr-str (apply join pieces)))))))))
                  on-fail on-partial start end position sink ((or split seq)
                                                              (first values)))))))

(defn char-buffer [init-string]
  (CharBuffer/wrap (str init-string)))

(run-char-parser (<> ch ch) (char-buffer "Hello World!"))
(run-char-parser (item (<> ch ch ch) str) (char-buffer "Hello World!"))


(run-char-printer (<> ch ch) (CharBuffer/allocate 4)
                  "HF")
(run-char-printer (item (<> ch ch ch) str) (CharBuffer/allocate 4)
                  ["HFEF"])

(defn success [& vals]
  (cont* k (fn [& st]
             (apply (apply k st) vals))))

(defn fail [& error]
  (cont* k (fn [on-fail & st]
             (on-fail k st error))))


#_(def not-& (complement #{'&}))

#_(defmacro parser [[k & args]
                  [[source] & read-body]
                  [[sink value] & write-body]]
  (let [pos-args (take-while not-& args)
        dest-args (drop-while not-& args)
        dest-gensym (drop (count pos-args)
                          [`on-fail# `start# `end# `position#])
        [on-fail start end position]
        (concat pos-args dest-gensym)]
    `(letfn [(mkp# [~k]
               (reify
                 Continuation
                 (continue* [_# k#]
                   (mkp# (comp k# ~k)))
                 Parser
                 (p-read ~['_ k on-fail start end position source]
                   (let ~(if (seq dest-gensym)
                           [(vec dest-args) (vec dest-gensym)]
                           [])
                     ~@read-body))
                 (p-write ~['_ k on-fail start end position sink value]
                   (let ~(if (seq dest-gensym)
                           [(vec dest-args) (vec dest-gensym)]
                           [])
                     ~@write-body))))])))
