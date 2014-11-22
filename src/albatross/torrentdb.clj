(ns albatross.torrentdb
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [clj-http.client :as http]
            [ring.util.codec :refer [url-decode]]
            [bencode.metainfo.reader :as bencode]
            [korma.core :refer :all]
            [albatross.db :as db]
            [cheshire.core :refer [parse-string generate-string]]
            [clojure.walk :refer [keywordize-keys stringify-keys]]))

(timbre/refer-timbre)

(defn- symbolize-state [d]
 (update-in d [:state] keyword))

(defn- stringify-state [d]
  (update-in d [:state] name))

(defn- last-id [res]
  ((keyword "last-insert-rowid()") res))

(defn- prepare-torrent
  [t]
  (->
   t
   (assoc :files (generate-string (:files t)))
   (stringify-state)
   (db/underscore-keys)))

(defn parse-date [d]
  (if d (java.util.Date. d) d))

(defn- stale-date []
  (java.util.Date. (- (System/currentTimeMillis) (* 1000 60 24))))

(defn clear-stale-torrents []
  (delete torrents (where {:state "created" :updated_at [< (stale-date)]})))


(defn- transform-torrent
  [t]
  (->
   t
   (assoc :files (keywordize-keys (parse-string (:files t))))
   (symbolize-state)
   (db/deunderscore-keys)
   (update-in [:created-at] parse-date)
   (update-in [:updated-at] parse-date)))

(defentity torrents
  (prepare prepare-torrent)
  (transform transform-torrent))

(defn- bytes->torrent [bytes]
  (let [t (bencode/parse-metainfo bytes)]
    {:info-hash (clojure.string/lower-case (bencode/torrent-info-hash-str t))
     :name (bencode/torrent-name t)
     :state :created
     :files (keywordize-keys (bencode/torrent-files t))
     :size (bencode/torrent-size t)
     :bytes bytes
     :created-at (java.util.Date.)
     :updated-at (java.util.Date.)}))

(defn find-by-hash [hash]
  "Finds a torrent in the database by info hash"
  (first (select torrents
                 (where {:info_hash (clojure.string/lower-case hash)})
                 (limit 1))))

(defn update-torrent! [torrent]
  "Updates a torrent in the database"
  (update torrents (set-fields (assoc torrent :updated-at (java.util.Date.)))
          (where {:id (:id torrent)})))

(defn remove-torrent! [torrent]
  "Removes a torrent from the database"
  (delete torrents (where {:id (:id torrent)})))

(defn clear-db! []
  "Clear the torrent database"
  (delete torrents))

(defn search [pattern]
  "Filters db by name"
  (select torrents (where {:name [like pattern]})))

(defn by-state [state]
  "Filters by state"
  (select torrents (where {:state (name state)})))

(defn find-or-create-by-bytes [bytes]
  "Returns or creates and returns the torrent object contained within bytes"
  (let [t (bytes->torrent bytes)
        db-t (find-by-hash (:info-hash t))]
    (if db-t
      db-t
      (first (select torrents (where {:id (last-id (insert torrents (values t)))}) (limit 1))))))

(defn parse-magnet [magnet]
  "takes a string and returns a map of the hash and name"
  {:src magnet
   :hash (second (re-find #"urn:btih:([\w]{32,40})" magnet))
   :name (url-decode (second (re-find #"dn=(.*?)&" magnet)))
   :trackers (map url-decode (drop 1 (clojure.string/split magnet #"tr=" )))})

(defn magnet->torrent [m]
  "gets a torrent file from the public torrent cache"
  (info (str "fetching torrent for magnet:" (:name m)) )
  (try
    (:body
     (http/get (str "http://torcache.net/torrent/" (clojure.string/upper-case (:hash m)) ".torrent") {:as :byte-array}))
    (catch Exception e
      (info "no torrent found!")
      nil)))
