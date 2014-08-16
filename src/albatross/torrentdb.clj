(ns albatross.torrentdb
  (:require [cheshire.core :refer :all]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [clj-http.client :as http]
            [ring.util.codec :refer [url-decode]]
            [com.stuartsierra.component :as component]
            [bencode.metainfo.reader :as bencode]))

(timbre/refer-timbre)


(defn save-db! [this]
  (io! (with-open [writer (io/writer (:db-file this))]
         (.write writer (generate-string @(:db this))))))

(defn- torrent-disk-path [this torrent]
  (conj (:cache-dir this) (str (:name torrent) ".torrent")))

(defn- db-exists? [this]
  (.exists (:db-file this)))

(defn bytes->torrent [bytes]
  (let [t (bencode/parse-metainfo bytes)]
    {:hash (bencode/torrent-info-hash-str t)
     :name (bencode/torrent-name t)
     :state :created
     :files (bencode/torrent-files t)
     :size (bencode/torrent-size t)}))

(defn update-torrent [this torrent]
  (swap! (:db this) assoc (:hash torrent) torrent)
  (save-db! this))

(defn save-to-disk [this torrent ^bytes bytes]
  (debug "save-to-disk " torrent)
  (io!
   (with-open [f (io/output-stream (apply io/file (torrent-disk-path this torrent)))]
     (.write f bytes))))

(defn find-or-create-by-bytes [this bytes]
  (let [t (bytes->torrent bytes)]
    (when-not (contains? @(:db this) (:hash t))
      (save-to-disk this t bytes)
      (update-torrent this t))
    t))

(defn to-byte-array [file]
  (let [out (java.io.ByteArrayOutputStream.)]
    (with-open [in (io/input-stream file)]
      (io/copy in out))
    (.toByteArray out)))

(defn torrent->bytes [this torrent]
  (io!
   (to-byte-array (apply io/file (torrent-disk-path this torrent)))))

(defn hash-equals? [a b]
  (.equalsIgnoreCase ^String (:hash a) ^String (:hash b)))

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

(defn load-db [this]
  (with-open [reader (io/reader (:db-file this))]
    (into {} (for [[k v] (parse-stream reader)] [k (convert-json v)]))))

(defn load-or-create [this]
  (when (db-exists? this)
    (info "Loading database from disk!")
    (reset! (:db this) (load-db this))))

(defn hash->torrent [this hash]
  (get (:db this) hash))

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

(defn clear-db! [this]
  "Clear the torrent database"
  (io!
   (debug "Clearing DB")
   (when (db-exists? this)
     (.delete (:db-file this)))
   (reset! (:db this) {})))

(defn search [this pattern]
  "Filters db by name"
  (filter (fn [[k v]] (>= (.indexOf (:name v) pattern) 0)) @(:db this)))

(defn by-state [this state]
  "Filters db by state"
  (filter (fn [[k v]] (= (:state v) state)) @(:db this)))

(defn remove-torrent! [this torrent]
  "Removes a torrent from the database"
  (debug "Removing torrent: " (:name torrent))
  (swap! (:db this) dissoc (:hash torrent))
  (save-db! this))

(defn get-torrent [this hash]
  "returns a torrent by hash"
  (get @(:db this) hash))

(defrecord TorrentDatabase [config db]
  component/Lifecycle

  (start [this]
    (info "loading torrent database")
    (load-or-create this)
    this)

  (stop [this]
    (info "closing torrent database")
    this))

(defn new-torrent-db [config]
  (map->TorrentDatabase {:db (atom {})
                         :home-dir (:home-dir config)
                         :db-file (io/file (:home-dir config) "torrentdb.json")
                         :cache-dir [(:home-dir config) "torrent-cache"] }))
