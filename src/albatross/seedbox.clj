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
   :is_open
   :is_private])

(defn- request-params [this]
  {:basic-auth (:credentials this) :insecure? true :debug false})

(defn- call [this cmd data]
  (xmlrpc/call* (:endpoint-url this) cmd [data] :request (request-params this)))

(def ^:private get-torrent-attributes
  (map #(str "d." (name %) "=") torrent-attributes))

(defn- torrent-list-by-hash [torrent-dicts]
  (zipmap (map #(%1 "hash") torrent-dicts) torrent-dicts))

(defn- torrent-list-to-map [torrent-list]
  (map (partial zipmap torrent-attributes) torrent-list))

(defn list-torrents [this]
  (torrent-list-to-map (call this :d.multicall
                             (conj get-torrent-attributes "main"))))

(defn- action-to-call [action]
  (keyword (str "d." (name action))))

(defn cmd [this action hash]
  (call this (action-to-call action) hash))

(defn cmd-p [this action hash]
  "same as send but implicity compares the result against the string 1"
  (= "1" (cmd this action hash)))

(defn send-to [this t]
  (infof "sending %s[%d] to seedbox" (:id t) (:name t))
  (call this :load_raw_start t))

(defn is-complete? [this torrent]
  (cmd-p this :complete (:info-hash torrent)))

(defrecord Seedbox [credentials endpoint-url]
  component/Lifecycle

  (start [this]
    this)

  (stop [this]
    this))

(defn create-seedbox [config]
  (map->Seedbox {:credentials [(get-in config [:rtorrent :username]) (get-in config [:rtorrent :password])]
                 :endpoint-url (get-in config [:rtorrent :endpoint-url])}))
