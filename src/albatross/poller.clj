(ns albatross.poller
  (:require [albatross.torrentdb :as db]
            [taoensso.timbre :as timbre]
            [clojure.core.async :refer [close! timeout chan go alt!]]
            [albatross.seedbox :as seedbox]
            [com.stuartsierra.component :as component]))

(timbre/refer-timbre)

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

(def ^:private poll-sleep-time 5000)

(def ^:private get-polling-torrents
  (partial db/by-state :seedbox))

(defn- check-seedbox [this]
  (doseq [t (get-polling-torrents)]
    (when (seedbox/is-complete? (:seedbox this) t)
      (db/update-torrent! (assoc t :state :ready-to-download))
      (<! (:channel (:downloader this)) t))))

(defn- poller-fn [this]
  (poll-go-loop [stop-timeout]
                (try
                  (check-seedbox this)
                  (catch Exception e
                    (warn e)))
                (sleep poll-sleep-time stop-timeout)))

(defrecord Poller [seedbox downloader poller]
  component/Lifecycle
  (start [this]
    (if-not poller
      (assoc :poller (poller-fn this))
      this))

  (stop [this]
    (when poller
      (close! poller)
      (dissoc this :poller))
    this))

(defn create-poller []
  (->Poller))
