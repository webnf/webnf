(ns webnf.date
  (:import (java.util Locale TimeZone Date)
           (java.text SimpleDateFormat DateFormat)))

(defn thread-local-date-format
  "Make a thread-safe DateFormat from a DateFormat factory fn

  SimpleDateFormat is not thread-safe, so we use a ThreadLocal proxy for access.
  http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335"
  [make-df] (proxy [ThreadLocal] []
              (initialValue [] (make-df))))

(defn date-formatter
  "Make a thread-safe formatter function from a DateFormat factory fn

  SimpleDateFormat is not thread-safe, so we use a ThreadLocal proxy for access.
  http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335"
  [tldf] (fn format-date [date]
           (.format ^DateFormat (.get ^ThreadLocal tldf) date)))

(defn date-parser
  "Make a thread-safe parser function from a DateFormat factory fn

  SimpleDateFormat is not thread-safe, so we use a ThreadLocal proxy for access.
  http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335"
  [tldf] (fn parse-date [string]
           (.parse ^DateFormat (.get ^ThreadLocal tldf) string)))

(defn simple-date-formatter
  "Make a thread-safe date formatter based on SimpleDateFormat"
  ([fmt] (simple-date-formatter fmt Locale/US (TimeZone/getTimeZone "GMT")))
  ([fmt locale timezone]
   (date-formatter
    (thread-local-date-format
     #(doto (SimpleDateFormat. fmt ^Locale locale)
        (.setTimeZone timezone))))))

(defn simple-date-parser
  "Make a thread-safe date-parser based on SimpleDateFormat"
  ([fmt] (simple-date-parser fmt Locale/US (TimeZone/getTimeZone "GMT")))
  ([fmt locale timezone]
   (date-parser
    (thread-local-date-format
     #(doto (SimpleDateFormat. fmt ^Locale locale)
        (.setTimeZone timezone))))))

;; RFC3339 says to use -00:00 when the timezone is unknown (+00:00 implies a known GMT)
(def format-utc  (simple-date-formatter "yyyy-MM-dd'T'HH:mm:ss.SSS-00:00"))
(def format-http (simple-date-formatter "EEE, dd MMM yyyy HH:mm:ss z"))
