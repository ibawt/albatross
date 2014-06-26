(ns albatross.providers.piratebay
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [albatross.providers.utils :as utils]
            [taoensso.timbre :as timbre]
            [albatross.torrentdb :as db]
            [albatross.torrent :as torrent]))

(defn extract-links [html]
  (map #(get-in %1 [:attrs :href]) html))

(defn filter-for-magnets [links]
  (filter #(>= (.indexOf % "magnet") 0) links))

(defn make-magnet-link [m]
  ; TODO lets not hardcode the port and host
  (str "http://localhost:3000/torrents/" (:name m) ".torrent?hash=" (:hash m)))

(defn load-from-disk [t m]
  (prn "loading from disk")
  (let [b (torrent/torrent->bytes t)]
    (assoc m :torrent b)))

(defn download-and-save [m]
  (when-let [b (db/magnet->torrent m)]
    (prn b)
    (torrent/save-to-disk (torrent/bytes->torrent b) b)
    (assoc m :torrent b)))

(defn fetch-magnet [m]
  (let [t (db/hash->torrent (:hash m))]
    (if t
      (load-from-disk t m)
      (download-and-save m))))

(defn search-piratebay [params]
  (->
   (http/get "https://thepiratebay.se/s/"
             {:query-params {:q (utils/make-search-query params)}})
   (:body)
   (html/html-snippet)
   (#(html/select % [[:a]]))
   (extract-links)
   (filter-for-magnets)
   (#(map db/parse-magnet %1))
   (#(map fetch-magnet %1))
   (#(remove nil? %1))
   (save-magnet-torrents)
   (#(map make-magnet-link %1))
   ))

(def config
  {:search-show search-piratebay
   :name "The Pirate Bay"
   :backlog? false
   :magnet true
   })
