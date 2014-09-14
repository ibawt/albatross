(ns albatross.poller
  (:require [albatross.torrentdb :as db]
            [taoensso.timbre :as timbre]
            [clj-http.client :as http]
            [clojure.core.async :refer [go <! >! <!! chan unique thread close! timeout]]
            [albatross.seedbox :as seedbox]
            [albatross.downloader :as downloader]
            [com.stuartsierra.component :as component]))

(timbre/refer-timbre)

(def ^:private poll-sleep-time 5000)

(def ^:private get-polling-torrents
  (partial db/by-state :seedbox))

(defn- check-seedbox [this]
  (doseq [t (get-polling-torrents)]
    (if (seedbox/is-complete? (:seedbox this) t)
      (db/update-torrent! (assoc t :state :ready-to-download))

      )))

(defn- poller-fn [this]
  (go
    (while @(:running this)
      (try
        (info "checking seedbox")
        (<! (timeout poll-sleep-time))
        (info "after timeout")
        (catch Exception e
          (warn e "Caught exception in poller"))))
    (info "out of while loop")))

(defrecord Poller [seedbox downloader running poller]
  component/Lifecycle
  (start [this]
    (if-not @running
      (do
        (reset! running true)
        (let [p (poller-fn this)]
          (info "p: "p)
          (assoc this :poller p)))
      this))

  (stop [this]
    (when running
      (close! poller)
      (reset! running false))
    this))

(defn create-poller []
  (map->Poller {:running (atom false)}))
