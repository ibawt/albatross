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
            t (db/find-by-hash t-hash)]
        (when (seedbox/is-complete? (:seedbox this) t)
          (info "PollJob " (:name t) " is complete!")
          (db/update-torrent! (assoc t :state :ready-to-download))
          (>! (get-in this [:downloader :channel]) t-hash))))))

(def get-polling-torrents
  (partial db/by-state :seedbox))

(def get-ready-torrents
  (partial db/by-state :ready-to-download))

(defn- poller [this]
  (go
    (while @(:running this)
      (try
        (<! (timeout 5000))
        (let [torrents (get-polling-torrents this)]
          (doseq [[k t] torrents]
            (>! (:channel this) (:hash t))))
        (doseq [[k t] (get-ready-torrents this)]
          (info "Sending " (:name t) " to download queue")
          (>! (:channel (:downloader this)) (:hash t)))
        (catch Exception e
          (warn e))))))

(defrecord Poller [downloader seedbox running channel]
  component/Lifecycle
  (start [this]
    (when-not running
      (reset! running true)
      (poller this)
      (poll-job this))
    this)

  (stop [this]
    (when running
      (reset! running false))
    this))

(defn create-poller []
  (map->Poller {:running (atom false)
                :channel (unique (chan 10))}))
