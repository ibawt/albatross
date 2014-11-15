(ns albatross.provider
  (:require [clj-http.client :as http]
            [environ.core :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [taoensso.timbre :as timbre]
            [albatross.providers.btn :as btn]
            [albatross.providers.iptorrents :as iptorrents]
            [albatross.providers.piratebay :as piratebay]
            [com.stuartsierra.component :as component]))

(timbre/refer-timbre)

(defn add-provider [this provider]
  (info (format "Adding a provider: %s" (:name provider)))
  (assoc (:providers this) assoc (:name provider) provider))

(defn torrent-from-hash [h]
  (str "http://torcache.net/torrent/" h ".torrent"))

(defn filter-config [this type]
  (filter type (vals (:providers this))))

;;; TODO complete
(defn fetch-rss [this]
  "returns a list of rss-urls"
  (:body (http/get (:rss-url (first (filter-config this :rss-url))))))

;;; TODO clean this up
(defn search-show [this params]
  "returns a list of urls to retreive the torrent at"
  (try
    (info params)
    (let [results
          (flatten (map #((:search-show %1) %1 params) (filter-config this :search-show)))]
      (info results)
      (clojure.string/join "," results))
    (catch Exception e
      (warn e))))


(defrecord Provider [config providers]
  component/Lifecycle

  (start [this]
    (if (nil? providers) ; just so we don't hammer iptorrents from the repl
      (assoc this :providers {:btn (btn/create config)
                              :iptorrents (iptorrents/create config)
                              :piratebay (piratebay/create)})
      this))

  (stop [this]
    this))

(defn create-provider [config]
  (map->Provider {:config config}))
