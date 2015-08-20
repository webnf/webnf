(ns webnf.cljs.enlive.transform
  (:require [webnf.cljs.enlive :as sel]))

(defmacro at [node-or-nodes & {:as rules}]
  `(lockstep-transform!
    (sel/as-nodes ~node-or-nodes)
    ~(into {} (for [[s t] rules]
                [(if (sel/static-selector? s) (sel/cacheable s) s)
                 t]))))

(defmacro instantiate [node & {:as rules}]
  `(let [inst# (.cloneNode ~node true)]
     (at inst#
         ~@(apply concat rules))
     inst#))

(defmacro clone-for [seq-comprehension & body]
  (let [rules (if (= 1 (count body))
                (cons `sel/root body)
                body)]
    `(fn [node#]
       (let [frag# (.createDocumentFragment js/document)]
         (doseq ~seq-comprehension
           (.appendChild frag# (instantiate node# ~@rules)))
         (goog.dom/replaceNode frag# node#)))))

(defmacro delegate [type sel-step argt & body]
  `(fn [node#]
     (webnf.event/delegate node# ~type (sel/compile-step ~sel-step)
                           (fn ~argt ~@body))))

(defmacro defsnippet [name path selector params & tf]
  `(let [snip-holder# (sel/load-html ~path :selector ~selector)]
     (defn ~name ~params
       (let [tf# #(at % ~@tf)]
         (cljs.core.async.macros/go
          (tf# (cljs.core.async/<! snip-holder#)))))))
