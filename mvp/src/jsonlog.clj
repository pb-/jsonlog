(ns jsonlog
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io]))

(def valid-log-id #"\w{16,}")

(def db
  {:dbtype "mysql"
   :dbname (System/getenv "MYSQL_DATABASE")
   :user (System/getenv "MYSQL_USER")
   :password (System/getenv "MYSQL_PASSWORD")
   :host (System/getenv "MYSQL_HOST")
   :port (or (System/getenv "MYSQL_PORT") "3306")})

(defn creation-schema []
  (slurp (clojure.java.io/resource "schema.sql")))

(defn parse-long
  ([string]
   (parse-long string nil))
  ([string default]
   (try
     (Long/parseLong string)
     (catch NumberFormatException _ default))))

(defn respond-json [status body]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (json/write-str body)})

(defn respond-error [status code detail]
  (respond-json status {:code code :detail detail}))

(defn next-offset [db-spec log-id]
  (->> ["SELECT COALESCE(MAX(offset) + 1, 0) AS offset FROM logs WHERE log_id = ?" log-id]
       (jdbc/query db-spec)
       first
       :offset))

(defn handle-store [request log-id]
  (let [query-offset (parse-long (get-in request [:params "offset"]))
        offset (next-offset (:db request) log-id)
        lines (line-seq (clojure.java.io/reader (:body request)))]
    (if (and query-offset (not= query-offset offset))
      (respond-error 409 :offset-mismatch "The expected offset does not match the actual offset.")
      (do (doseq [[i line] (map vector (range) lines)]
            (jdbc/insert! (:db request) :logs {:log_id log-id :offset (+ offset i) :data line}))
          (respond-json 201 {:offset offset})))))

(defn handle-retrieve [request log-id]
  (let [query-offset (parse-long (get-in request [:params "from"]) 0)
        offset (if (neg? query-offset) (+ (next-offset (:db request) log-id) query-offset) query-offset)
        ; TODO make query lazy
        result (jdbc/query (:db request) ["SELECT offset, data FROM logs WHERE log_id = ? AND offset >= ? ORDER BY offset" log-id offset])
        lines (map #(str (:data %) \newline) result)
        headers (if-not (empty? lines) {"Jsonlog-Offset" (str (:offset (first result)))} {})]
    {:status 200
     :headers (merge {"Content-Type" "text/plain"} headers)
     :body lines}))

(defn handler [request]
  (let [log-id (subs (:uri request) 1)]
    (if-not (re-matches valid-log-id log-id)
      (respond-error 400 :invalid-log-id "The log id is invalid.")
      (case (:request-method request)
        :get (handle-retrieve request log-id)
        :post (handle-store request log-id)
        (respond-error 501 :not-implemented "The requested method is not implemented.")))))

(defn wrap-txn [handler-func db-spec]
  (fn [request]
    (jdbc/with-db-transaction [txn db-spec]
      (handler-func (assoc request :db txn)))))

(def wrapped-handler
  (-> handler
      wrap-params
      (wrap-txn db)))

(defn retry
  ([func]
   (retry func [1 2 3 5 5 5 5 5]))
  ([func delays]
   (try
     (func)
     (catch Exception e
       (if (empty? delays)
         (throw e)
         (do
           (Thread/sleep (* (first delays) 1000))
           (retry func (rest delays))))))))

(defn -main []
  (println "waiting for database...")
  (retry #(jdbc/db-do-commands db (creation-schema)))
  (println "starting server")
  (run-jetty wrapped-handler {:port 3000}))

(comment
  (do ; run this for hot reloading development
    (jdbc/db-do-commands db (creation-schema))
    (require '[ring.middleware.reload :refer [wrap-reload]])
    (run-jetty (wrap-reload #'wrapped-handler) {:port 3000 :join? false})))
