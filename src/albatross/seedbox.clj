(ns albatross.seedbox
  (:require [clj-http.client :as http]
            [environ.core :refer :all]
            [necessary-evil.core :as xmlrpc]
            [taoensso.timbre :as timbre]
            [environ.core :refer :all]
            [com.stuartsierra.component :as component]))

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

(defn- request-params [this]
  {:basic-auth (:credentials this) :insecure? true :debug false})

(defn- call [this cmd data]
  (xmlrpc/call* (:endpoint-url this) cmd [data] :request (request-params this)))

(defn send-to [this t]
  (call this :load_raw_start t))

(defn is-complete? [this torrent]
  (= "1" (call :d.complete (clojure.string/upper-case (:hash torrent)))))

(def get-torrent-attributes
  (vec (map #(str "d." % "=") torrent-attributes)))

(defn torrent-list-by-hash [torrent-dicts]
  (zipmap (map #(%1 "hash") torrent-dicts) torrent-dicts))

(defn torrent-list-to-map [torrent-list]
  (map #(into {} (map vector torrent-attributes %1)) torrent-list))

(def list-torrent-arguments
  (vec (concat ["main"] get-torrent-attributes)))

;; (defn list-torrents []
;;  (xmlrpc/call* endpoint-url "d.multicall" list-torrent-arguments :request request-params))

;; ;; TODO make this work
;; (defn get-torrent-by-hash [hash]
;;   "doesn't work yet"
;;   (xmlrpc/call* endpoint-url "system.multicall" (map #(hash-map "methodName" % "param" hash) get-torrent-attributes) :request request-params))

;; (defn get-all-remote-torrents []
;;  (->
;;   (list-torrents)
;;   torrent-list-to-map
;;   torrent-list-by-hash))

(defrecord Seedbox [credentials endpoint-url]
  component/Lifecycle

  (start [this]
    (info "Starting Seedbox")
    this)

  (stop [this]
    (info "Stopping Seedbox")
    this))

(defn create-seedbox [config]
  (map->Seedbox {:credentials [(get-in config [:rtorrent :username]) (get-in config [:rtorrent :password])]
                 :endpoint-url [(get-in config [:rtorrent :endpoint-url])]}))
