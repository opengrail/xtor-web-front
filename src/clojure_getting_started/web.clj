(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [datomic.api :as d]
            [cheshire.core :refer :all]
            [compojure.route :as route]))


(def customer-schema [{:db/id                 #db/id[:db.part/db]
                       :db/ident              :person/shared-id
                       :db/valueType          :db.type/uuid
                       :db/cardinality        :db.cardinality/one
                       :db/unique             :db.unique/value
                       :db/doc                "The UUID of the person created by the data management tool"
                       :db.install/_attribute :db.part/db}

                      {:db/id                 #db/id[:db.part/db]
                       :db/ident              :person/first-name
                       :db/valueType          :db.type/string
                       :db/cardinality        :db.cardinality/one
                       :db/doc                "The first name of the person"
                       :db.install/_attribute :db.part/db}

                      {:db/id                 #db/id[:db.part/db]
                       :db/ident              :person/last-name
                       :db/valueType          :db.type/string
                       :db/cardinality        :db.cardinality/one
                       :db/doc                "The last name of the person"
                       :db.install/_attribute :db.part/db}])


(defn create-schema [conn]
  @(d/transact conn customer-schema))

(defn insert-data [conn customer]
  @(d/transact conn customer))

; anthropomorphise storage - why not
(defn populate [conn]
  (create-schema conn)
  (insert-data conn [{:db/id             #db/id [:db.part/user -1]
                      :person/shared-id  (java.util.UUID/randomUUID)
                      :person/first-name "Hello"
                      :person/last-name  (str "Datomic" (rand-int 1337))}]))

(defn fetch-ids [conn]
  (d/q '[:find ?id .
         :in $ ?person-name
         :where [?id :person/first-name ?person-name]] (d/db conn) "Hello"))

(defn random-id [conn]
  (rand-nth (flatten (map identity (fetch-ids conn)))))

(defn query-data [conn]
  (let [db (d/db conn)]
    (d/pull db '[*] (random-id conn))))

;DYNAMO_DATABASE_URL
;datomic:ddb://us-east-1/your-system-name/heroku-spaces

(def dynamo-db "DYNAMODB")
(def storage (or (env :datomic-storage-type) :heroku_postgres))

;DATOMIC_STORAGE_TYPE
; DYNAMODB or HEROKU_POSTGRES (default) or POSTGRES

(defn connect-dynamodb []
  (d/connect (env :dynamo-database-url)))

(defn connect-heroku-postgres []
  (let [jdbc-url (env :jdbc-database-url)
        ssl-params "&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
        db-url (str "datomic:sql://datomic?" jdbc-url ssl-params)]
    (d/connect db-url)))

(defn get-conn []
  (if (= storage dynamo-db)
    (connect-dynamodb)
    (connect-heroku-postgres)))

(defn greeting []
  (time
    (query-data (get-conn))))

(defn get-customer []
  (time
    (let [conn (get-conn)]
      (populate conn)
      (query-data conn))))

(defn datomic-hello []
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (pr-str ["Quick greeting from a lightly populated Datomic ... " (greeting) "runnning on" storage])})

(defn splash []
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (pr-str ["Getting the party started on Datomic ... " (get-customer) "running on" storage])})

(defroutes app
           (GET "/" []
             (splash))
           (GET "/hello" []
             (datomic-hello))
           (ANY "*" []
             (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
