(ns webnf.date
  (:import (java.util Locale TimeZone Date)
           (java.text SimpleDateFormat DateFormat)))

(defn thread-local-date-format
  "Make a thread-safe formatter function from a DateFormat factory fn
  
  SimpleDateFormat is not thread-safe, so we use a ThreadLocal proxy for access.
  http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335"
  ([make-df]
   (let [tldf (proxy [ThreadLocal] []
                (initialValue [] (make-df)))]
     (fn format-date [date]
       (.format ^DateFormat (.get ^ThreadLocal tldf) date)))))

(defn simple-date-format
  "Make a thread-safe date formatter based on SimpleDateFormat"
  ([fmt] (simple-date-format fmt Locale/US (TimeZone/getTimeZone "GMT")))
  ([fmt locale timezone]
   (thread-local-date-format
    #(doto (SimpleDateFormat. fmt ^Locale locale)
       (.setTimeZone timezone)))))

;; RFC3339 says to use -00:00 when the timezone is unknown (+00:00 implies a known GMT)
(def format-utc  (simple-date-format "yyyy-MM-dd'T'HH:mm:ss.SSS-00:00"))
(def format-http (simple-date-format "EEE, dd MMM yyyy HH:mm:ss z"))
