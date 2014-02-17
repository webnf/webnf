(ns webnf.middleware.pretty-exception)

;; taken from https://github.com/weejulius/raiseup

(defn extract-file-name-and-number
  [^String error-stack]
  (if-not (empty? error-stack)
    (let [ns-index (.indexOf error-stack "$")
          namespace (if (>= ns-index 0)
                      (.substring error-stack 0 ns-index))
          ln-index (.lastIndexOf error-stack ":")
          ln (if (>= ln-index 0)
               (.substring error-stack (inc ln-index) (dec (.length error-stack))))]
      (if-not (or (empty? namespace) (= "ring.middleware.pretty_exception" namespace))
        [(str (.replace namespace "." "/") ".clj") (Long/parseLong ln)]))))

(defn print-source-code
  [source-file-name error-line-num]
  (if-not (empty? source-file-name)
    (let [path (str (System/getProperty "user.dir") "/src/" source-file-name)
          from-line (- error-line-num 15)
          to-line (+ error-line-num 15)
          error-line-num (dec error-line-num)]
      (if (.exists (java.io.File. ^String path))
        (with-open [rdr (clojure.java.io/reader path)]
          (loop [lines (line-seq rdr)
                 index 0
                 source-code ""]
            (if (and (not (empty? lines)) (<= index to-line))
              (recur (rest lines) (inc index)
                     (if (< index from-line)
                       source-code
                       (str source-code
                            "<li"
                            (if (= index error-line-num) " class=\"error-line\"")
                            ">"
                            "<span class=\"ln\">" (inc index) "</span>"
                            (-> (first lines)
                                (.replace " " "&nbsp")
                                (str "</br></li>")))))
              (str "<pre><h3>" source-file-name "</h3><ul>" source-code "</ul></pre>"))))))))

(defn pretty-print-exception
  "pretty exception"
  [e]
  (str
    "<html>
      <head>
        <title> Oops, goes wrong </title>
        <style type=\"text/css\">
          html {
           background:black;
           color:white;
           font-size:85%;
           font-family: Consolas, \"Liberation Mono\", Courier, monospace
          }
          .msg h1,.msg h3{
            font-weight : 800;
            font-size : 1em;
            line-height:40px;
            padding:10px 0 10px 20px;
            background:green;
          }
          .msg h3{
            font-weight :600;
            font-size:0.8em;
            line-height:30px;
            margin-top:10px;
          }
          pre {
            margin:20px;
          }
          .error-line{
           background:#a60000;
           font-weight:800;
           margin: 10px 0;
           font-size :120%;
           padding:10px 0;
          }
          ul{
            list-style-type:none;
          }
          .ln{
            margin-right:10px;
          }
          .stack{
           text-align : right;
           padding-right:30px;
          }
        </style>
      </header>
   <body>"
    (apply str
           (map #(if-not (nil? %)
                  (str "<div class=\"msg\"><h1>"
                       (if (instance? clojure.lang.ExceptionInfo %)
                         (str (.getMessage %) "<h3>" (.getData %) "</h3>")
                         e)
                       "</h1></div>")) [e (.getCause e)]))
    "<div>"
    "<ul class=\"stacks\">"
    (apply str
           (map
             #(str "<li class=\"stack\">" % "</li>"
                   (if-let [file-and-ln (extract-file-name-and-number (str %))]
                     (apply print-source-code file-and-ln)))
             (.getStackTrace ^Exception e)))
    "</ul></div>"
    " </body>
    </html>"))

(defn wrap-pretty-exception
  "catch exception and pretty print"
  [handler & [opts]]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (merge request
               {:body
                         (pretty-print-exception e)
                :status  500
                :headers {"Content-Type" "text/html"}})))))
