(defproject clojure-getting-started "1.0.0-SNAPSHOT"
  :description "Demo Clojure web app"
  :url "http://clojure-getting-started.herokuapp.com"
  :license {:name "Eclipse Public License v1.0"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"my.datomic.com" {:url      "https://my.datomic.com/repo"
                                   :username :env
                                   :password :env}}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [cheshire "5.5.0"]
                 [environ "1.0.1"]
                 [org.postgresql/postgresql "9.3-1102-jdbc41"]
                 [com.taoensso/carmine "2.12.0"]
                 [com.datomic/datomic-pro "0.9.5302" :exclusions [joda-time]]]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "clojure-getting-started-standalone.jar"
  :profiles {:production {:env {:production true}}})
