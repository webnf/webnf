(ns webnf.date
  (:import (java.util Locale TimeZone Date)
           (java.text SimpleDateFormat)))

(defn thread-local-simple-date-format
  ([fmt] (thread-local-simple-date-format fmt Locale/US (TimeZone/getTimeZone "GMT")))
  ([fmt locale timezone]
   ;; SimpleDateFormat is not thread-safe, so we use a ThreadLocal proxy for access.
   ;; http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335
   (proxy [ThreadLocal] []
     (initialValue []
       (doto (SimpleDateFormat. fmt)
         ;; RFC3339 says to use -00:00 when the timezone is unknown (+00:00 implies a known GMT)
         (.setTimeZone timezone))))))

(defn simple-date-format
  ([fmt] (simple-date-format fmt Locale/US (TimeZone/getTimeZone "GMT")))
  ([fmt locale timezone]
   (let [dtf (thread-local-simple-date-format fmt locale timezone)]
     (fn format [date]
       (.format ^SimpleDateFormat (.get ^ThreadLocal dtf) date)))))

(def format-utc  (simple-date-format "yyyy-MM-dd'T'HH:mm:ss.SSS-00:00"))
(def format-http (simple-date-format "EEE, dd MMM yyyy HH:mm:ss z"))
