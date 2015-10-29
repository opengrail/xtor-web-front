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

(def customer [{:db/id             #db/id [:db.part/user -1]
                :person/shared-id  #uuid "d213198b-36b5-4c19-8cb1-e172f59091d9"
                :person/first-name "Oscar"
                :person/last-name  "Fistorious"}])

(defn create-schema [conn]
  @(d/transact conn customer-schema))

(defn insert-data [conn customer]
  @(d/transact conn customer))

; Connect to the database when this namespace is loaded
(def db-map (let [jdbc-url (env :jdbc-database-url)
                  ssl-params "&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
                  db-uri (str "datomic:sql://datomic?" jdbc-url ssl-params)

                  ; created! (d/create-database db-uri)

                  conn (d/connect db-uri)
                  db (d/db conn)

                  ; schema! (create-schema conn)
                  ; insert! (insert-data conn customer)
                  ]
              {:db db :conn conn}))

(def oscar [:person/shared-id #uuid "d213198b-36b5-4c19-8cb1-e172f59091d9"])

(defn query-data [db]
  (d/pull db [:person/first-name :person/last-name] oscar))

(defn get-customer []
  (query-data (:db db-map)))

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
