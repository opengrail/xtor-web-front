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

(def storage-type (if-let [storage (env :datomic-storage-type)]
                    storage
                    :heroku_postgres))

(def customer [{:db/id             #db/id [:db.part/user -1]
                :person/shared-id  #uuid "d213198b-36b5-4c19-8cb1-e172f59091d9"
                :person/first-name "Hello"
                :person/last-name  (str "Datomic on " storage-type)}])

(defn create-schema [conn]
  @(d/transact conn customer-schema))

(defn insert-data [conn customer]
  @(d/transact conn customer))

; anthropomorphise storage - why not
(def hello [:person/shared-id #uuid "d213198b-36b5-4c19-8cb1-e172f59091d9"])

(defn populate [conn]
  (create-schema conn)
  (insert-data conn customer))

(defn query-data [conn]
  (let [db (d/db conn)]
    (d/pull db [:person/first-name :person/last-name] hello)))

;DYNAMO_DATABASE_URL
;datomic:ddb://us-east-1/your-system-name/heroku-spaces

;DATOMIC_STORAGE_TYPE
; DYNAMODB or HEROKU_POSTGRES (default) or POSTGRES

; Connect to the database when this namespace is loaded (so far HEROKU_POSTGRES and DYNAMODB)

(defn dynamodb []
  (let [db-url (env :dynamo-database-url)]
    ((d/create-database db-url)
      (d/connect db-url))))

(defn heroku-postgres []
  (let [jdbc-url (env :jdbc-database-url)
        ssl-params "&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
        db-url (str "datomic:sql://datomic?" jdbc-url ssl-params)]
    (d/create-database db-url)
    (d/connect db-url)))

(defn get-conn []
  (if (= storage-type :heroku_postgres)
    (heroku-postgres)
    (dynamodb)))

(defn get-customer []
  (let [conn (get-conn)]
    (populate conn)
    (query-data conn)))

(defn splash []
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (pr-str ["Hello" (get-customer)])})

(defroutes app
           (GET "/" []
             (splash))
           (ANY "*" []
             (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
