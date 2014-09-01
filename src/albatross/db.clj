(ns albatross.db
  (:require [clojure.java.jdbc :as jdbc]
            [korma.db :refer :all]
            [korma.core :refer :all]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(defn- sanitize-entities [t]
  (.replace ^String t "-" "_"))

(defn underscoreize [k]
  (keyword (clojure.string/replace (name k) "-" "_")))

(defn deunderscorize [k]
  (keyword (clojure.string/replace (name k) "_" "-")))

(defn underscore-keys [m]
  (into {} (for [[k v] m] [(underscoreize k) v])))

(defn deunderscore-keys [m]
  (into {} (for [[k v] m] [(deunderscorize k) v])))

(def ^:private create-series
  "creates the table to contain tvdb information"
  [(jdbc/create-table-ddl :series
                           [:id "integer" :primary :key]
                           [:language "varchar(255)"]
                           [:name "varchar(255)"]
                           [:aliases "text"]
                           [:banner "varchar(512)"]
                           [:overview "text"]
                           [:first-aired "date"]
                           [:network "varchar(255)"]
                           [:tvdb-id "integer"]
                           [:imdb-id "varchar(255)"]
                           [:zap2it-id "varchar(255)"]
                           :entities sanitize-entities)])

(def ^:private create-episodes
  "creates the table to contain tvdb episode information"
  [(jdbc/create-table-ddl :episodes
                           [:id "integer" :primary :key]
                           [:tvdb-id "integer"]
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
                           :entities sanitize-entities)])

(def ^:private create-myshows
  "table for my shows"
  [(jdbc/create-table-ddl :myshows
                           [:id "integer" :primary :key]
                           [:series-id "integer"]
                           [:rename-pattern "varchar(255)"]
                           :entities sanitize-entities)])

(def ^:private create-myepisodes
  "table for my episodes"
  [(jdbc/create-table-ddl :myepisodes
                           [:id "integer" :primary :key]
                           [:myshow-id "integer"]
                           [:episode-id "integer"]
                           [:state "varchar(255)"]
                           :entities sanitize-entities)])

(def ^:private create-torrents
  "table for torrents"
  [(jdbc/create-table-ddl :torrents
                           [:id "integer" :primary :key]
                           [:info-hash "varchar(255)"]
                           [:name "varchar(512)"]
                           [:state "varchar(255)"]
                           [:files "varchar(4096)"]
                           [:size "integer"]
                           [:bytes "blob"]
                           [:created-at "DATETIME"]
                           [:updated-at "DATETIME"]
                           :entities sanitize-entities)
   "CREATE UNIQUE INDEX hash_idx on torrents(info_hash)"
   "CREATE INDEX state_idx on torrents(state)"])

(defn- create-table [db-spec table-cmds]
  (try
    (apply jdbc/db-do-commands db-spec table-cmds)
    (catch java.sql.SQLException e)))

(def ^:private tables
  [create-series create-episodes create-torrents create-myshows create-myepisodes])

(defn- create-tables [db-spec]
  (doseq [t tables]
    (create-table db-spec t)))

(defrecord Database [config db-spec db]
  component/Lifecycle
  (start [this]
    (let [db-spec (sqlite3 {:db (:db-file config)})
          db (create-db db-spec)]
      (default-connection db)
      (create-tables db-spec)
      (merge this {:db-spec db-spec :db db})))
  (stop [this]
    this))

(defn create-database [config]
  (map->Database {:config config}))
