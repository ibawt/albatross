(ns albatross.seedbox
  (:require [clj-http.client :as http]
            [environ.core :refer :all]
            [necessary-evil.core :as xmlrpc]
            [taoensso.timbre :as timbre]
            [environ.core :refer :all]
            [albatross.torrent :as torrent]))

(timbre/refer-timbre)

(def torrent-attributes
  [:name
   :hash
   :connection_current
   :size_bytes
   :completed_bytes
   :creation_date
   :bytes_done
   :up.rate
   :down.rate
   :get_message
   :peers_complete
   :peers_connected
   :state
   :complete
   :is_active
   :is_hash_checked
   :is_multi_file
   :is_open])

(def credentials
  [(env :rtorrent-username)
   (env :rtorrent-password)])

(def request-params
  {:basic-auth credentials :insecure? true :debug false})

(def endpoint-url
  (env :rtorrents-xmlrpc-url))

(defn- call [cmd data]
  (xmlrpc/call* endpoint-url cmd [data] :request request-params))

(defn send-to [t]
  (call :load_raw_start t))

(defn is-complete? [torrent]
  (= "1" (call :d.complete (clojure.string/upper-case (:hash torrent)))))

(def get-torrent-attributes
  (vec (map #(str "d." % "=") torrent-attributes)))

(defn torrent-list-by-hash [torrent-dicts]
  (zipmap (map #(%1 "hash") torrent-dicts) torrent-dicts))

(defn torrent-list-to-map [torrent-list]
  (map #(into {} (map vector torrent-attributes %1)) torrent-list))

(def list-torrent-arguments
  (vec (concat ["main"] get-torrent-attributes)))

(defn list-torrents []
  (xmlrpc/call* endpoint-url "d.multicall" list-torrent-arguments :request request-params))

(defn get-torrent-by-hash [hash]
  "doesn't work yet"
  (xmlrpc/call* endpoint-url "system.multicall" (map #(hash-map "methodName" % "param" hash) get-torrent-attributes) :request request-params))

(defn get-all-remote-torrents []
  (->
   (list-torrents)
   torrent-list-to-map
   torrent-list-by-hash))
