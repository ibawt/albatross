(ns albatross.handler
  (:require [clojure.core.async :as async]
            [compojure.core :refer :all]
            [ring.adapter.jetty :as jetty]
            [compojure.route :as route]
            [ring.server.standalone :as ring]
            [ring.middleware.params :refer :all]
            [albatross.torrentdb :as db]
            [albatross.seedbox :as seedbox]
            [albatross.provider :as provider]
            [ring.middleware.json :as middleware-json]
            [albatross.poller :as poller]
            [albatross.downloader :as downloader]
            [ring.middleware.defaults :refer :all]
            [ring.util.response :refer [resource-response response]]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [albatross.myshows :as myshows]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]))

(timbre/refer-timbre)

(def home-dir
  "we place our torrents here"
  (.getAbsolutePath (clojure.java.io/file (System/getProperty "user.home") "Torrents")))

(let [tor-dir (io/file home-dir)]
  (when-not (.exists tor-dir)
    (.mkdir tor-dir)))

; TODO the sending and changing should be abstracted out of this
(defn send-torrent-from-post
  "end point to receive a base64 encoded torrent in the file parameter"
  [seedbox request]
  (info "send-torrent-from-post: " request)
  (let [decoded-torrent (ring.util.codec/base64-decode (:file request))]
    (db/update-torrent!
     (assoc (db/find-or-create-by-bytes decoded-torrent) :state :seedbox))
    (seedbox/send-to seedbox decoded-torrent)
    "OK"))

(defn torrent-by-hash [params]
  (->
   (:hash params)
   (db/find-by-hash)
   (:bytes)
   (java.io.ByteArrayInputStream.)))

(defn app-routes [provider seedbox]
  (try
    (routes
     (POST "/search" {params :params} (provider/search-show provider params))
     (POST "/send_torrent" {params :params} (send-torrent-from-post seedbox params))
     (GET "/torrents/:id" {params :params} (torrent-by-hash params))
     (GET "/rss" request (provider/fetch-rss provider))

     (route/resources "/")
     (route/not-found "Not Found"))
    (catch Exception e
      (info e "Caught exception in routes"))))

(def my-site-defaults
  (dissoc site-defaults :security))

(defn app [provider seedbox]
  (wrap-defaults (app-routes provider seedbox) my-site-defaults))

(defrecord HTTPServer [port server provider seedbox]
  component/Lifecycle
  (start [this]
    (if-not server
      (let [s (ring/serve (app provider seedbox)
                                   {:port port
                                    :open-browser? false
                                    :stacktraces? true})]
        (assoc this :server s))
      this))

  (stop [this]
    (when server
      (info "stopping server")
      (.stop server))
    (assoc this :server nil)))

(defn create-http-server [{port :port}]
  (map->HTTPServer {:port port}))
