(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [taoensso.carmine :as redis :refer (wcar)]
            [datomic.api :as d]
            [cheshire.core :refer :all]
            [compojure.route :as route]))


(defn- get-redis-connection-pool []
  (let [spec {:pool {} :spec (if-let [uri (env :redis-url)]
                               {:uri uri}
                               {:host "127.0.0.1" :port 6379})}]
    spec))

(defn- look-up-datomic [list-name]
  (if-let [value (redis/wcar (get-redis-connection-pool) (redis/lindex list-name 0))]
    (parse-string value true)))

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
  (let [datomic (look-up-datomic "datomic")
        jdbc-url (env :jdbc-database-url)
        credentials (get-jdbc-credentials jdbc-url)
        jdbc-params (str "?username=" (:username credentials) "&password=" (:password credentials))
        ssl-params "ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"

        simple-jdbc (first (clojure.string/split jdbc-url #"\?"))
        conn-map {:protocol          :sql
                  :db-name           "datomic"
                  :sql-driver-params (str jdbc-params "&" ssl-params)
                  ;:username (:username credentials)
                  ;:password (:password credentials)
                  :sql-url           simple-jdbc}
        created! (d/create-database conn-map)
        conn (d/connect conn-map)
        db (d/db conn)
        ]
    db))

(def db (db-connect))

(defn splash []
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (pr-str ["Hello" :from 'Heroku])})

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
