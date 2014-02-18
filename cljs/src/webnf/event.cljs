(ns webnf.event
  (:require
   [goog.dom :as dom]
   [goog.events :as evt]))

;; delegation helpers

(defn delimited-parents
  "Get a sequence of parent elements up to top-element (including element and top-element)"
  [element top-element]
  (when element
    (cons element
          (when-not (identical? element top-element)
            (lazy-seq
             (delimited-parents (dom/getParentElement element) top-element))))))

(defn delegate
  "Set an event handler on element that bubbles events from a child that received event, up to itself.
   Calls handler on every elemtent "
  [element event select-descendant? handler]
  (evt/listen element event
              (fn [evt]
                (doseq [dsc (delimited-parents (.-target evt) element)]
                  (when (select-descendant? dsc)
                    (handler dsc evt))))))
