(ns albatross.poller
  (:require [albatross.torrentdb :as db]
            [taoensso.timbre :as timbre]
            [clj-http.client :as http]
            [clojure.core.async :refer [go <! >! chan timeout alts! unique]]
            [albatross.seedbox :as seedbox]
            [albatross.downloader :as downloader]
            [com.stuartsierra.component :as component]))

(timbre/refer-timbre)

(defn- poll-job [this]
  (go
    (while @(:running this)
      (let [t-hash (<! (:channel this))
            t (get (db/get-torrent (:torrent-db this) t-hash))]
        (when (seedbox/is-complete? (:seedbox this) t)
          (info "PollJob " (:name t) " is complete!")
          (db/update-torrent (assoc t :state :ready-to-download))
          (>! (get-in this [:downloader :channel]) t-hash))))))

(defn- get-polling-torrents [this]
  (db/by-state (:torrent-db this) :seedbox))

(defn- get-ready-torrents [this]
  (db/by-state (:torrent-db this) :ready-to-download))

(defn- poller [this]
  (go
    (while @(:running this)
      (<! (timeout 5000))
      (let [torrents (get-polling-torrents this)]
        (doseq [[k t] torrents]
          (>! (:channel this) (:hash t))))
      (doseq [[k t] (get-ready-torrents this)]
        (info "Sending " (:name t) " to download queue")
        (>! (:channel (:downloader this)) (:hash t))))))

(defrecord Poller [downloader torrent-db seedbox running channel]
  component/Lifecycle
  (start [this]
    (info "Starting Poller")
    (when-not running
      (reset! running true)
      (poller this)
      (poll-job this))
    this)

  (stop [this]
    (info "Stopping Poller")
    (when running
      (reset! running false))
    this))

(defn create-poller []
  (map->Poller {:running (atom false)
                :channel (unique (chan 10))}))
