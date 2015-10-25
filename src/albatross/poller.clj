(ns albatross.poller
  (:require [albatross.torrentdb :as db]
            [taoensso.timbre :as timbre]
            [albatross.downloader :as downloader]
            [clojure.core.async :refer
             [close! timeout chan go alt! >! >!! alts!]]
            [albatross.seedbox :as seedbox]
            [com.stuartsierra.component :as component]))

(timbre/refer-timbre)

(defmacro dbg [& body]
  `(let [x# ~body]
     (println (quote ~body) "=" x#) x#))

(defmacro sleep
  ([ms stop-channel]
     `(alts! [(timeout ~ms) ~stop-channel]))
  ([ms]
     `(<! (timeout ~ms))))

(defmacro poll-go-loop [bindings & body]
  (let [stop (first bindings)]
    `(let [~stop (chan)]
       (go (while (alt! ~stop false :default :keep-going)
             ~@body))
       ~stop)))

(def ^:private poll-sleep-time (* 5 60 1000))

(def get-polling-torrents
  (partial db/by-state :seedbox))

(defn- check-seedbox [this]
  (doseq [t (get-polling-torrents)]
    (infof "polling[%d]: %s" (:id t) (:name t))
    (when (seedbox/is-complete? (:seedbox this) t)
      (let [t-done (assoc t :state :ready-to-download)]
        (db/update-torrent! t-done)
        (infof "sending %s to download queue" (:name t-done))
        (go (>! (:download-queue (:downloader this)) t-done))))))

(defn- filter-not-in-db [this torrents]
  (filter #(not (db/find-by-hash (:hash %))) torrents))

(defn- add-to-db [this torrents]
  (doseq [t torrents]
    (-> (db/find-or-create-by-bytes (downloader/get-torrent-file (:downloader this) (:hash t)))
        (assoc :state :seedbox)
        (db/update-torrent!))))

(defn sync-torrentdb [this]
  (let [torrents (seedbox/list-torrents (:seedbox this))]
    (->> torrents
         (filter-not-in-db this)
         (add-to-db this))))

(defn- poller-fn [this]
  (poll-go-loop [stop-timeout]
                (try
                  (info "polling...")
                  (check-seedbox this)
                  (db/clear-stale-torrents)
                  (sync-torrentdb this)
                  (catch Exception e
                    (warn e)))
                (sleep poll-sleep-time stop-timeout)))

(defn wake [this]
  (go (>! (:poller this))))

(defrecord Poller [seedbox downloader poller]
  component/Lifecycle
  (start [this]
    (if-not poller
      (assoc this :poller (poller-fn this))
      this))

  (stop [this]
    (if poller
      (do (close! poller)
          (assoc this :poller nil))
      this)))

(defn create-poller []
  (map->Poller {}))
