(ns albatross.torrentdb
  (:require [cheshire.core :refer :all]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [clj-http.client :as http]))

(timbre/refer-timbre)

(def db (atom {}))

(defn db-file []
  (io/file (System/getProperty "user.home") "Torrents" "torrentdb.json"))

(defn db-exists? []
  (.exists (db-file)))

(defn symbolize-state [d]
  (assoc d :state (keyword (:state d))))

(defn symbolize-params [params]
  (into {} (for [[k v] params] [(keyword k) v])))

(defn load-db []
  (with-open [reader (io/reader (db-file))]
    (into {} (for [[k v] (parse-stream reader)] [k (symbolize-state (symbolize-params v))]))))

(defn load-or-create []
  (when (db-exists?)
    (info "Loading database from disk!")
    (reset! db (load-db))))

(defn save-db! []
  (io! (with-open [writer (io/writer (db-file))]
         (.write writer (generate-string @db)))))

(defn update-torrent [torrent]
  (debug "Updating torrent: " (:name torrent))
  (swap! db assoc (:hash torrent) torrent)
  (save-db!))

(defn hash->torrent [hash]
  (get @db hash))

(defn parse-magnet [magnet]
  "takes a string and returns a map of the hash and name"
  {:src magnet
   :hash (second (re-find #"urn:btih:([\w]{32,40})" magnet))
   :name (second (re-find #"dn=(.*?)&" magnet))})

(defn magnet->torrent [m]
  "gets a torrent file from the public torrent cache"
  (info (str "fetching torrent for magnet:" (:name m)) )
  (try
    (->
     ; does torcache use https??
     ; we should use the other fallback in case it's not here
     (http/get (str "http://torcache.net/torrent/" (clojure.string/upper-case (:hash m)) ".torrent") {:as :byte-array})
     (:body))
    (catch Exception e
      (info "no torrent found!")
      nil)))

(defn clear-db! []
  (io!
   (debug "Clearing DB")
   (when (db-exists?)
     (.delete (db-file)))
   (reset! db {})))

(defn search [pattern]
  (filter (fn [[k v]] (>= (.indexOf (:name v) pattern) 0)) @db))

(defn by-state [state]
  (filter (fn [[k v]] (= (:state v) state)) @db))

(defn remove-torrent! [torrent]
  (debug "Removing torrent: " (:name torrent))
  (swap! db dissoc (:hash torrent))
  (save-db!))

(load-or-create)
