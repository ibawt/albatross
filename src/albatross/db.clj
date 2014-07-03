(ns albatross.db
  (:require [clojure.java.jdbc :as jdbc]
            [korma.db :refer :all]
            [korma.core :refer :all]))

(def dbspec {:classname "org.sqlite.JDBC"
             :subprotocol "sqlite"
             :subname "albatross.db"})

(defdb db (sqlite3 {:db "albatross.db"}))

;; TODO we use these method in more than one place
(defn- sanitize-entities [t]
  (.replace ^String t "-" "_"))

(def create-series
  "creates the table to contain tvdb information"
  (jdbc/create-table-ddl :series
                         [:id "integer" :primary :key]
                         [:language "varchar(255)"]
                         [:name "varchar(255)"]
                         [:aliases "text"]
                         [:banner "varchar(512)"]
                         [:overview "text"]
                         [:first-aired "date"]
                         [:network "varchar(255)"]
                         [:tvdb-id "integer" :unique]
                         [:imdb-id "varchar(255)" :unique]
                         [:zap2it-id "varchar(255)" :unique]
                         :entities #(.replace ^String % "-" "_")))

(def create-episodes
  "creates the table to contain tvdb episode information"
  (jdbc/create-table-ddl :episodes
                         [:id "integer" :serial :primary :key]
                         [:tvdb-id "integer" :unique]
                         [:name "varchar(255)"]
                         [:number "integer"]
                         [:first-aired "date"]
                         [:imdb-id "varchar(255)"]
                         [:overview "text"]
                         [:season "integer"]
                         [:production-code "varchar(255)"]
                         [:rating "decimal"]
                         [:rating-count "integer"]
                         [:series-id "integer"]
                         [:season-id "integer"]
                         [:filename "varchar(255)"]
                         [:thumb-width "integer"]
                         [:thumb-height "integer"]
                         :entities #(.replace ^String % "-" "_")))

(def create-myshows
  "table for my shows"
  (jdbc/create-table-ddl :myshows
                         [:id "integer" :primary :key]
                         [:series-id "integer"]
                         [:rename-pattern "varchar(255)"]
                         :entities sanitize-entities))

(def create-myepisodes
  "table for my episodes"
  (jdbc/create-table-ddl :myepisodes
                         [:id "integer" :primary :key]
                         [:myshow-id "integer"]
                         [:episode-id "integer"]
                         [:state "varchar(255)"]
                         :entities sanitize-entities))

(defn create-table [table]
  (try
    (jdbc/db-do-commands dbspec table)
    (catch Exception e)))

(doseq [t [create-series create-episodes create-myshows create-myepisodes]]
  (create-table t))
