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

(defn- download-and-save [m]
  (when-let [b (db/magnet->torrent m)]
    (assoc m :torrent (db/find-or-create-by-bytes b))))

(defn- fetch-magnet [m]
  (let [t (db/find-by-hash (:hash m))]
    (if t
      t
      (download-and-save m))))

(defn- search-piratebay [config params]
  (->
   (http/get "https://thepiratebay.se/s/"
             {:query-params {:q (utils/make-search-query params)}})
   (:body)
   (html/html-snippet)
   (#(html/select % [[:a]]))
   (extract-links)
   (filter-for-magnets)
   (#(map db/parse-magnet %1))
   (#(pmap fetch-magnet %1))
   (#(remove nil? %1))
   (#(map make-magnet-link %1))))

(def ^:private config
  {:search-show search-piratebay
   :name "The Pirate Bay"
   :backlog? false
   :magnet true})

(defn create []
  config)
