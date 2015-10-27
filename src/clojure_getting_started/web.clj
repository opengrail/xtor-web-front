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

(defn- set-jdbc-credentials! [url]
  (let [user-fields (-> (clojure.string/split url #"\?")
                        (last)
                        (clojure.string/split #"&")
                        (first)
                        (clojure.string/split #"="))
        user-key (first user-fields)
        user-value (last user-fields)
        password-fields (-> (clojure.string/split url #"&")
                            (last)
                            (clojure.string/split #"="))
        password-key (first password-fields)
        password-value (last password-fields)]
    (if (= "user" user-key)
      (System/setProperty "datomic.sqlUser" user-value))
    (if (= "password" password-key)
      (System/setProperty "datomic.sqlPassword" password-value))
    (System/setProperty "datomic.sqlDriverParams" "ssl=true;sslfactory=org.postgresql.ssl.NonValidatingFactory")))

(defn db-connect []
  (let [datomic (look-up-datomic "datomic")
        jdbc-url (env :jdbc-database-url)
        properties-set! (set-jdbc-credentials! jdbc-url)
        simple-jdbc (first (clojure.string/split jdbc-url #"\?"))
        uri (str "datomic:sql://customer?" simple-jdbc)
        conn-map {:protocol :sql
                  :db-name "customer"
                  :sql-driver-params "ssl=true;sslfactory=org.postgresql.ssl.NonValidatingFactory"}
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
