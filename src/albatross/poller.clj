(ns albatross.poller
  (:require [albatross.torrentdb :as db]
            [taoensso.timbre :as timbre]
            [clj-http.client :as http]
            [clojure.core.async :refer [go <! >! chan timeout alts! unique]]
            [albatross.seedbox :as seedbox]
            [albatross.downloader :as downloader]))

(timbre/refer-timbre)

(def ^:private keep-polling (atom true))

(def num-channels 10)

(def channel (unique (chan num-channels)))

(defn poll-job []
  (go
    (while @keep-polling
      (let [t-hash (<! channel)
            t (get @db/db t-hash)]
        (when (seedbox/is-complete? (get @db/db t-hash))
          (info "PollJob " (:name t) " is complete!")
          (db/update-torrent (assoc (get @db/db t-hash) :state :ready-to-download))
          (>! downloader/download-channel t-hash))))))

(defn get-torrents-for-state [state]
  (filter (fn [[k v]] (= (:state v) state)) @db/db))

(defn get-polling-torrents []
  (db/by-state :seedbox))

(defn get-ready-torrents []
  (db/by-state :ready-to-download))

(defn- poller []
  (go
    (while @keep-polling
      (<! (timeout 5000))
      (let [torrents (get-polling-torrents)]
        (doseq [[k t] torrents]
          (>! channel (:hash t))))
      (doseq [[k t] (get-ready-torrents)]
        (info "Sending " (:name t) " to download queue")
        (>! downloader/download-channel (:hash t))))))

(defn start-poller []
  (reset! keep-polling true)
  (poller)
  (poll-job))

(defn stop-poller []
  (reset! keep-polling false))
