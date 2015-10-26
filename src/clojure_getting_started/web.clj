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

(defn db-connect []
  (let [datomic (look-up-datomic "datomic")
        uri (str "datomic:sql://" (:host datomic) ":" (:port datomic) "/customer"
                 "?" (env :jdbc-database-url)
                 "&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory")
        created (d/create-database uri)
        conn (d/connect uri)
        db (d/db conn)]
    db))

; TODO: WTF - need to provide JDBC URL??

(def db (db-connect))

(clojure.pprint/pprint db)

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (pr-str ["Hello" :from 'Heroku])})

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
