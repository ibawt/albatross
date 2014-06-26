(ns albatross.handler
  (:require [clojure.core.async :as async]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :refer :all]
            [albatross.torrentdb :as db]
            [albatross.torrent :as torrent]
            [albatross.seedbox :as seedbox]
            [albatross.provider :as provider]
            [ring.middleware.json :as middleware-json]
            [albatross.poller :as poller]
            [albatross.downloader :as downloader]
            [ring.middleware.defaults :refer :all]
            [ring.util.response :refer [resource-response response]]
            [albatross.views.layout :as layout]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(def home-dir (.getAbsolutePath (clojure.java.io/file (System/getProperty "user.home") "Torrents")))

(defn send-torrent-from-post
  [request]
  (let [decoded-torrent (ring.util.codec/base64-decode (request "file"))]
    (torrent/find-or-create-by-bytes decoded-torrent)
    (seedbox/send-to decoded-torrent)
    "OK"))

(defn init []
  (info "Albatross -- INIT")
  (db/load-or-create)
  (poller/start-poller)
  (downloader/downloader-job))

(defn destroy []
  (info "Albatross -- destroy"))

(defroutes app-routes
  (GET "/" [] (apply str (albatross.views.layout/layout (layout/home 1))))
  (GET "/torrents/:id" {params :params} (provider/magnet-torrent-by-hash params))
  (GET "/rss" request (provider/fetch-rss))
  (POST "/search" {params :params} (provider/search-show params))
  (POST "/send_torrent" {params :params} (send-torrent-from-post params))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
