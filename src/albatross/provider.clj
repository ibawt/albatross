(ns albatross.provider
  (:require [clj-http.client :as http]
            [environ.core :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [taoensso.timbre :as timbre]
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
  (let [results
        (flatten (map #((:search-show %1) %1 params) (filter-config this :search-show)))]
    (info results)
    (clojure.string/join "," results)))


(defrecord Provider [config providers]
  component/Lifecycle

  (start [this]
    (info "Starting providers")
    (if (nil? providers) ; just so we don't hammer iptorrents from the repl
      (assoc this :providers {:iptorrents (iptorrents/create config)
                              :piratebay (piratebay/create)})
      this))

  (stop [this]
    (info "Stopping providers")
    this))

(defn create-provider [config]
  (map->Provider {:config config}))

; this should go somewhere else
(def test-params {:description
                  "Penny squares off with a new beautiful female neighbor who may become the building's \"new Penny.\"",
                  :name "The Dead Hooker Juxtaposition",
                  :hastbn "False",
                  :hasnfo "False",
                  :airdate "733496",
                  :file_size "0",
                  :release_name "",
                  :season "5",
                  :show_name "The Big Bang Theory",
                  :status "5",
                  :episode "19",
                  :tvdbid "461831",
                  :location "",
                  :showid "80379"})
