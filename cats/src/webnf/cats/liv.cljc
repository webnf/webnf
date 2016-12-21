(ns webnf.cats.liv
  (:require [webnf.cats.katie :refer [pure <* *> <$> <*> <>]]
            [webnf.cats.connie :refer
             [cont* cont** defcontinuation* defcontinuation continue* get-cc run-cont run-cont*
              map-cont* map-cont with-cont* cfn cont-fn fn-cont ccomp]]
            [webnf.cats.monie :refer [mdo >>= id]]
            [clojure.tools.logging :as log]))


(let [pure (cont-fn cont-pure [k & vals]
                    (apply k vals))
      cfmap (fn cont-cfmap [c cf]
              (run-cont* c cf))
      join (cont-fn cont-join [k cc]
                    (run-cont* cc #(run-cont* % k)))]
  (defn run-cont-m [cm final]
    (continue* (cm pure cfmap join) final)))

(defn m-pure [& vals]
  (fn [pure cfmap join]
    (apply pure vals)))

(defn m-cfmap [m cf]
  (fn [pure cfmap join]
    (cfmap (m pure cfmap join)
           cf)))

(defn m-join [mm]
  (fn [pure cfmap join]
    (join (mm pure cfmap join))))

(defn m-fmap [m f]
  (m-cfmap m (fn [& vals] (cont* k (k (apply f vals))))))

(defn m-insert [& mv]
  (fn [_ _ _]
    (apply pure mv)))

;; seq monad

(let [zip-lists (fn [arity conts]
                  (for [i (range arity)]
                    (for [c conts]
                      (run-cont* c (fn [& args] (nth args i))))))]
  (defn seq-t [m]
    (fn [base-pure base-cfmap base-join]
      (letfn [(seq-pure [& vals]
                (apply base-pure (map list vals)))
              (seq-cfmap [sm cf]
                (base-cfmap
                 sm (cont-fn [k & lists]
                             (apply k (let [ls (apply map cf lists)
                                            arity (continue* (first ls) (fn [& args] (count args)))]
                                        (zip-lists arity ls))))))
              (seq-join [ssm]
                (base-cfmap
                 ssm (cont-fn [k & lists]
                              (apply k (for [nls lists]
                                         (apply concat nls))))))]
        (m seq-pure
           seq-cfmap
           seq-join)))))

;; state monad

(def state-get
  (cont* k (fn [& state]
             (apply (apply k state) state))))

(defn state-set [& new-state]
  (cont* k (fn [& state]
             (apply (apply k state) new-state))))

(defn state-t [m]
  (fn [base-pure base-cfmap base-join]
    (m (fn state-pure [& vals]
         (base-pure
          (fn [& state])))
       (fn state-cfmap [sm cf]
         (base-cfmap
          sm (fn [sf]
               )))
       (fn state-join [ssm]
         ))))


(comment

  (-> (m-insert [1 2])
      (m-fmap inc)
      seq-t
      (run-cont-m identity))

  (-> (m-insert [10 20] [100 200])
      ;;(m-fmap inc)
      ;;(m-fmap (fn [x y] [(inc x) (dec x) (+ x y)]))
      (m-cfmap (cont-fn [k x y] (k [(inc x) (dec x) (+ x y)]
                                   [(inc y) (dec y) (- x y)])))
      m-join
      seq-t
      (run-cont-m vector))

  (-> (m-pure 1 2)
      seq-t
      (run-cont-m vector))

  (-> (m-pure 1 2 3)
      (m-cfmap (cont-fn [k a b c] (k #{(inc a) (inc b) (inc c)}
                                     #{(dec a) (dec b) (dec c)}
                                     #{(+ a b c) (+ a b) (+ b c)})))
      m-join
      seq-t
      (run-cont-m vector))

  )
#_(comment
    ;; seq-m
    (def -seq-m
      (letfn [(pure [c]
                (cont* k (continue*
                          c (fn [& vals]
                              (apply k (map list vals))))))
              (fmap [f c]
                (cont* k (continue*
                          c (fn [& lists]
                              (k (apply map f lists))))))
              (join [lcsc]
                (cont* k (continue*
                          lcsc (fn [lcs]
                                 (if (empty? (log/spy lcs))
                                   (k)
                                   (apply
                                    k (reduce
                                       (fn [lists lc]
                                         (continue* (lc pure fmap join)
                                                    (fn [& nlists]
                                                      (if (not= (count lists) (count nlists))
                                                        (throw (ex-info "Arity mismatch" {:base-lists lists
                                                                                          :next-lists nlists}))
                                                        (mapv concat lists nlists)))))
                                       (continue* ((first lcs) pure fmap join)
                                                  vector)
                                       (next lcs))))))))]
        {:pure pure :fmap fmap :join join}))

    (defn as-seq-m [& vals]
      (fn [_ _ _]
        (cont* k (apply k vals))))

    (defn -pure-m [& vals]
      (fn [pure fmap join]
        (pure (cont* k (apply k vals)))))

    (defn -fmap-m [mc cf]
      (fn [pure fmap join]
        #_(fmap f (mc pure fmap join))
        (cont* k (continue* (fmap cf (mc pure fmap join))
                            (fn [])))))

    (defn -join-m [mmc]
      (fn [pure fmap join]
        (join (mmc pure fmap join))))

    (defn -bind-m [mc f]
      (-join-m (-fmap-m mc f))
      #_(fn [pure fmap join]
          ((-join-m (fmap f (mc pure fmap join))) pure fmap join)))


    (defn -run-m [mv {:keys [pure fmap join]} final]
      (continue* (mv pure fmap join) final))


    #_(-> #_(-pure-m 1 2 3)
          (as-seq-m [1 2 3] [4 5 6])
          #_(-bind-m (fn [a b] (-pure-m {:a1 a :b1 b}
                                        {:a2 a :b2 b})))
          #_(-fmap-m (fn [& maps] (map #(assoc % :seen true) maps)))
          (-fmap-m (cont-fn [k & vals] (k (apply + vals)
                                          (apply - vals))))
          (-run-m -seq-m vector))
    )
#_(comment
    (defcontinuation Monad -monad [k pure fmap join]
      (continue* (k pure fmap join)
                 (cont-fn [vk & vals]
                          (cont*
                           j (-> (apply fmap j vals)
                                 (continue* join)
                                 (continue* vk))))))

    (defn run-m [m final & mv]
      (continue* m (cont-fn [k pure fmap join]
                            (continue* (apply k mv) final))))

    (def seq-m
      (-monad
       (cont-fn [k & vals] (apply k (map list vals)))
       (cont-fn [k f & lists]
                ;; (println "fmap" k f lists #_(pr-str (apply map f lists)))
                (k (apply map f lists)))
       (cont-fn [k & nested]
                ;; (println "nested" k (pr-str nested) #_(pr-str (map (partial apply concat) nested)))
                (apply k (map (partial apply concat) nested)))))

    (run-cont (run-m seq-m identity [1 2 3] [4 5 6]) (comp list hash-set))

    (defcontinuation BindM bind-m [k m f]
      (continue*
       m (fn [pure fmap join]
           (continue*
            (k pure fmap join)
            (cont-fn [vk & vals]
                     (-> (apply fmap f vals)
                         (continue* (cfn j join))
                         (continue* vk)
                         (->> (cont* j))))))))

    (-> seq-m

        (bind-m (fn [a b] [[:b1 a b]]))
    
        (run-m identity [1 2 3] [4 5 6])
        (run-cont list))
    )
#_(comment

  

    (run-either (run-m either-m (right 1))
                #(vector :left %&)
                #(vector :right %&))

    )

#_(comment

    (defn b* [m f]
      (continue* m ()))


    (defn left [& vals]
      (cont* k (fn [lk] (apply lk vals))))

    (defn right [& vals]
      (cont* k (fn [lk] ((apply k vals) lk))))


    (defn run-either [either left-f right-f]
      ((continue*
        either
        (fn [& vals]
          (fn [_] (apply right-f vals))))
       left-f))

    (defn either-t [m left-k]
      (cont*
       k (continue* m #(run-either % left-k k))))

    #_(defn mapcat-cps [f & ls]
        (cont* k
               (println 'MCCPS f ls)
                                        ;(k (apply mapcat f ls))
               (continue* ls (comp k (partial apply mapcat f)))))

    (declare seq-m run-seq)

    (defcontinuation SeqM -seq-m [f seqs]
      (println f seqs)
      (cont* k (k (seq-m (apply mapcat f seqs)))))

    (defn seq-m [& ls]
      (-seq-m ls))

    (defn run-seq [sm]
      (first (:seqs (continue* (continue* sm list)
                               identity))))

    (defn seq-t [m]
      (cont*
       k (continue* m (fn [& sqs]
                        (apply mapcat (comp k seq-t) sqs)
                        #_(k (apply seq-m sqs)))
                    #_(apply concat (apply map k %&))
                    #_#(apply mapcat k %&))))


    (comment

      (continue* (continue* (seq-m [1 2 3]) list) identity)

      (run-seq (seq-m [1 2 3]))

      (-> (seq-m [1 2 3])
          (>>= (comp seq-m (juxt inc dec)))
          (run-cont* list) (run-cont*) :seqs doall
                                        ;run-seq
          )
  
      (run-cont* (mapcat-cps (comp list inc) ( [1 2 3])))
  
      (-> (juxt inc dec)
          (mapcat-cps [1 2 3])
          run-cont*)

      (-> (seq-m [1 2 3])
          (>>= (fn [a & args]
                 (println a args)
                 (seq-m [(dec a) (inc a)])))
          run-cont run-cont* first type)

      (-> (seq-m [1 2 3])
          (>>= (comp seq-m list inc))

          run-cont run-cont*)

      (->> (pure 4 5 6)
           (<$> +)
           run-cont*)

      (-> (mdo [[a b] (seq-m [1 2 3 4] [4 5 6 7])
                [x]   (seq-m [a '+ b '= (+ a b) '_])]
               (pure [a b x]))
          (run-cont)
          (run-cont))

      (-> (mdo [[a b] (seq-t (pure [1 2 3 4] [4 5 6 7]))
                [x]   (pure [a '+ b '= (+ a b) '_])]
               (pure [a b x]))
          (run-cont)
          #_(run-cont))
  
      )
    )
