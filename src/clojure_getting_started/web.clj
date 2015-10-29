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

(defn- get-jdbc-credentials [url]
  (let [user-fields (-> (clojure.string/split url #"\?")
                        (last)
                        (clojure.string/split #"&")
                        (first)
                        (clojure.string/split #"="))
        user-value (last user-fields)
        password-fields (-> (clojure.string/split url #"&")
                            (last)
                            (clojure.string/split #"="))
        password-value (last password-fields)]
    {:username user-value :password password-value}))

(defn db-connect []
  (let [
        ;jdbc-url (env :jdbc-database-url)
        ;credentials (get-jdbc-credentials jdbc-url)
        ;jdbc-params (str "?username=" (:username credentials) "&password=" (:password credentials))
        ;ssl-params "ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
        ;simple-jdbc (first (clojure.string/split jdbc-url #"\?"))
        ;conn-map {:protocol          :sql
        ;          :db-name           "datomic"
        ;          :sql-driver-params (str jdbc-params "&" ssl-params)
        ;          ;:username (:username credentials)
        ;          ;:password (:password credentials)
        ;          :sql-url           simple-jdbc}

        hard-coded "datomic:sql://datomic?jdbc:postgresql://ec2-107-21-219-142.compute-1.amazonaws.com:5432/dd7fmhk85j9m9d?user=dxdrdjqkrmsxpn&password=VAnW_4FQ86ks3NKZwHsMTsM0C2&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
        ;created! (d/create-database hard-coded)
        conn (d/connect hard-coded)
        db (d/db conn)]
    {:db db :conn conn}))

(defn create-schema [conn]
  @(d/transact conn customer-schema)
  (println "Created schema"))

(defn insert-data [conn customer]
  @(d/transact conn customer)
  (println "Inserted data"))

(def oscar [:person/shared-id #uuid "d213198b-36b5-4c19-8cb1-e172f59091d9"])

(defn query-data [db]
  (println "Query data")
  (d/pull db [:person/first-name :person/last-name] oscar))

(defn get-customer []
  (let [conn-map (db-connect)]
    ;(create-schema (:conn conn-map))
    ;(insert-data (:conn conn-map) customer)
    (query-data (:db conn-map))))

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
