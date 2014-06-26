(ns albatross.providers.iptorrents
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as timbre]
            [environ.core :refer :all]
            [albatross.providers.utils :as utils]))

(def ^:private cookie-store (clj-http.cookies/cookie-store))
(def ^:private torrents-url "https://iptorrents.com/torrents")
(def ^:private torrent-pass (env :iptorrents-torrent-pass))
(def ^:private torrent-credentials {:username (env :iptorrents-username)
                                    :password (env :iptorrents-password)})
(def ^:private login
  (http/post torrents-url {:form-params torrent-credentials
                          :cookie-store cookie-store}))
(def ^:private rss-url
  (env :iptorrents-rss-url))

(defn- parse-search-results [html]
  (->
   (html :body)
   (html/html-snippet)
   (#(html/select % [[:table.torrents] [:a]]))))

(defn- search-results [params]
  (http/get torrents-url
                {:query-params {:q (utils/make-search-query params)}
                 :cookie-store cookie-store}))

(defn- get-links [tags]
  (filter #(.contains ^String % "download")
          (map #(get-in % [:attrs :href]) tags)))


(defn- add-torrent-pass [links]
  (map #(str "https://iptorrents.com" (.replaceAll ^String % " " "%20") "?torrent_pass=" torrent-pass) links))

(defn search-show [params]
  (-> (search-results params)
      (parse-search-results)
      (get-links)
      (add-torrent-pass)))

(def config
  {:search-show search-show ; main search function
   :rss-url rss-url         ; rss
   :name "IPTorrents"       ; humanize
   :backlog? true          ; rss
   :magnet false            ; has magnetlinks
   })
