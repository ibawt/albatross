(ns albatross.provider
  (:require [clj-http.client :as http]
            [environ.core :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [taoensso.timbre :as timbre]
            [albatross.providers.iptorrents :as iptorrents]
            [albatross.providers.piratebay :as piratebay]
            ))

(timbre/refer-timbre)
(def providers
  "a map of name to providers"
  (atom {}))

(defn add-provider! [provider]
  (info (format "Adding a provider: %s" (:name provider)))
  (swap! providers assoc (:name provider) provider))

(defn torrent-from-hash [h]
  (str "http://torcache.net/torrent/" h ".torrent"))

(defn filter-config [type]
  (keep identity (map type (vals @providers))))

; TODO complete
(defn fetch-rss []
  "returns a list of rss-urls"
  (:body (http/get (first (filter-config :rss-url)))))

(defn search-show [params]
  "returns a list of urls to retreive the torrent at"
  (info "search-show: " params)
  (clojure.string/join "," (flatten
                            (map #(%1 params) (filter-config :search-show)))))

; TODO should be a config somewhere
(add-provider! albatross.providers.iptorrents/config)
(add-provider! albatross.providers.piratebay/config)

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
