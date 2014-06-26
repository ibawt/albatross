(ns albatross.torrent
  (:require [bencode.metainfo.reader :as bencode]
            [taoensso.timbre :as timbre]
            [albatross.torrentdb :as db]
            [clojure.java.io :as io]))
(timbre/refer-timbre)

(def home-dir (.getAbsolutePath (clojure.java.io/file (System/getProperty "user.home") "Torrents")))
(def cache-dir [home-dir "torrent-cache"])

(defn disk-path [torrent]
  (conj cache-dir (str (:name torrent) ".torrent")))

(defn bytes->torrent [bytes]
  (let [t (bencode/parse-metainfo bytes)]
    {:hash (bencode/torrent-info-hash-str t)
     :name (bencode/torrent-name t)
     :state :created
     :files (bencode/torrent-files t)
     :size (bencode/torrent-size t)}))

(defn to-byte-array [file]
  (let [out (java.io.ByteArrayOutputStream.)]
    (with-open [in (io/input-stream file)]
      (io/copy in out))
    (.toByteArray out)))

(defn torrent->bytes [torrent]
  (io!
   (to-byte-array (apply io/file (disk-path torrent)))))

(defn hash-equals? [a b]
  (.equalsIgnoreCase ^String (:hash a) ^String (:hash b)))

(defn save-to-disk [torrent ^bytes bytes]
  (debug "save-to-disk " torrent)
  (io!
   (with-open [f (io/output-stream (apply io/file (disk-path torrent)))]
     (.write f bytes))))

(defn find-or-create-by-bytes [bytes]
  (let [t (bytes->torrent bytes)]
    (when-not (contains? @db/db (:hash t))
      (save-to-disk t bytes)
      (db/update-torrent t))
    t))
