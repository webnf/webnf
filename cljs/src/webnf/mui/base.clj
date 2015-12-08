(ns webnf.mui.base)

(defmacro defwrapped [name orig [f attrs content] & body]
  `(let [bf# (let [~f ~orig]
               (fn [~attrs ~content] ~@body))]
     (defn ~name [& [a# & r# :as attrs+content#]]
       (if (map? a#)
         (bf# a# r#)
         (bf# {} attrs+content#)))))

(defmacro defelement* [name [cursor children] & body]
  `(defn ~(with-meta name
            {:arglists (list [cursor '& children]
                             ['& children])})
     [~'& [cfg# :as children#]]
     (let [have-cfg# (map? cfg#)
           ~cursor (if have-cfg# cfg# {})
           ~children (if have-cfg# (next children#) children#)]
       ~@body)))

(defmacro defcomponent [name [cursor children owner opts] & body]
  `(defelement* ~name [cursor# ~children]
     (om.core/build (fn [~cursor ~owner ~opts] ~@body)
                    cursor# {:opts {::component '~name}})))

(defmacro defcomponent-kv [name [cursor owner opts] & body]
  (let [cursor-sym (or (:as cursor) (gensym "cursor-"))]
    `(defn ~name [~'& ~(if (map? cursor)
                         (assoc cursor :as cursor-sym)
                         {:as cursor})]
       (om.core/build (fn [_# ~owner ~opts] ~@body) ~cursor-sym))))

(defmacro component
  "Sugar over reify for quickly putting together components that
   only need to implement om.core/IRender and don't need access to
   the owner argument."
  [display-name & body]
  `(reify
     om.core/IDisplayName
     (~'display-name [this#]
       ~display-name)
     #_#_om.core/IDidMount
     (~'did-mount [this#]
       (webnf.util/log "Mounted" ~display-name))
     om.core/IRender
     (~'render [this#]
       ~@body)))

(defmacro defelement [name [cursor children] & body]
  `(defcomponent ~name [~cursor ~children _# _#]
     (component ~(str name) ~@body)))
