(ns albatross.providers.iptorrents
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as timbre]
            [environ.core :refer :all]
            [albatross.providers.utils :as utils]))

(timbre/refer-timbre)
;; TODO make this part of the config
(def cookie-store (clj-http.cookies/cookie-store))

(def ^:private torrents-url "https://iptorrents.com/torrents")

(defn login [config]
  (http/post torrents-url {:form-params {:username (:username config) :password (:password config)}
                           :cookie-store cookie-store}))

(defn parse-search-results [html]
  (->
   (html :body)
   (html/html-snippet)
   (#(html/select % [[:table.torrents] [:a]]))))

(defn search-results [config params]
  (http/get torrents-url
            {:query-params {:q (utils/make-search-query params)}
             :cookie-store cookie-store}))

(defn get-links [tags]
  (filter #(.contains ^String % "download")
          (map #(get-in % [:attrs :href]) tags)))

(defn add-torrent-pass [links tp]
  (info "ADDING A TORRENT PASS" tp)
  (map #(str "https://iptorrents.com"
             (.replaceAll % " " "%20")
             "?torrent_pass="
             tp) links))

(defn search-show [config params]
  (-> (search-results config params)
      (parse-search-results)
      (get-links)
      (add-torrent-pass (:pass config))))

(def static-config
  {:search-show search-show ; main search function
   :name "IPTorrents"       ; humanize
   :backlog? true           ; rss
   :magnet false            ; has magnetlinks
   })

(defn create [{iptorrents :iptorrents}]
  (let [config (merge static-config iptorrents {:cooke-store (clj-http.cookies/cookie-store)})]
    (login config)
    config))
