(ns albatross.torrentdb
  (:require [cheshire.core :refer :all]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [clj-http.client :as http]
            [ring.util.codec :refer [url-decode]]))

(timbre/refer-timbre)

(def db (atom {}))

(defn db-file []
  (io/file (System/getProperty "user.home") "Torrents" "torrentdb.json"))

(defn db-exists? []
  (.exists (db-file)))

(defn symbolize-state [d]
 (update-in d [:state] keyword))

(defn symbolize-params [params]
  (into {} (for [[k v] params] [(keyword k) v])))

(defn convert-json
  "symbolizes the params and state value"
  [json]
  (->
   json
   (symbolize-params)
   (symbolize-state)))

(defn load-db []
  (with-open [reader (io/reader (db-file))]
    (into {} (for [[k v] (parse-stream reader)] [k (convert-json v)]))))

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
   :name (url-decode (second (re-find #"dn=(.*?)&" magnet)))
   :trackers (map url-decode (drop 1 (clojure.string/split magnet #"tr=" )))
   })

(defn magnet->torrent [m]
  "gets a torrent file from the public torrent cache"
  (info (str "fetching torrent for magnet:" (:name m)) )
  (try
    (:body
     (http/get (str "http://torcache.net/torrent/" (clojure.string/upper-case (:hash m)) ".torrent") {:as :byte-array}))
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
