(ns albatross.providers.piratebay
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [albatross.providers.utils :as utils]
            [taoensso.timbre :as timbre]
            [albatross.torrentdb :as db]))

(timbre/refer-timbre)

(defn- extract-links [html]
  (map #(get-in %1 [:attrs :href]) html))

(defn- filter-for-magnets [links]
  (filter #(>= (.indexOf % "magnet") 0) links))

(defn- make-magnet-link [m]
  ; TODO lets not hardcode the port and host
  (str "http://localhost:3000/torrents/" (:name m) ".torrent?hash=" (:hash m)))

(defn- load-from-disk [torrent-db t m]
  (let [b (db/torrent->bytes t)]
    (assoc m :torrent b)))

(defn- download-and-save [torrent-db m]
  (when-let [b (db/magnet->torrent m)]
    (let [t (db/bytes->torrent b)]
      (db/save-to-disk torrent-db t b)
      (db/update-torrent torrent-db t)
      (assoc m :torrent b))))

(defn- fetch-magnet [torrent-db m]
  (let [t (db/hash->torrent torrent-db (:hash m))]
    (if t
      (load-from-disk torrent-db t m)
      (download-and-save torrent-db m))))

(defn- search-piratebay [{torrent-db :torrent-db} params]
  (->
   (http/get "https://thepiratebay.se/s/"
             {:query-params {:q (utils/make-search-query params)}})
   (:body)
   (html/html-snippet)
   (#(html/select % [[:a]]))
   (extract-links)
   (filter-for-magnets)
   (#(map db/parse-magnet %1))
   (#(map (partial fetch-magnet torrent-db) %1))
   (#(remove nil? %1))
   (#(map make-magnet-link %1))))

(def ^:private config
  {:search-show search-piratebay
   :name "The Pirate Bay"
   :backlog? false
   :magnet true})

(defn create [torrent-db]
  (info "created piratebay provider")
  (merge config {:torrent-db torrent-db}))
