(ns albatross.handler
  (:require [clojure.core.async :as async]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :refer :all]
            [albatross.torrentdb :as db]
            [albatross.seedbox :as seedbox]
            [albatross.provider :as provider]
            [ring.middleware.json :as middleware-json]
            [albatross.poller :as poller]
            [albatross.downloader :as downloader]
            [ring.middleware.defaults :refer :all]
            [ring.util.response :refer [resource-response response]]
            [albatross.views.layout :as layout]
            [taoensso.timbre :as timbre]
            [ring.server.standalone :as server]
            [clojure.java.io :as io]
            [albatross.myshows :as myshows]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]))

(timbre/refer-timbre)

(defn- logger
  "custom logger without time"
  [{:keys [level throwable message timestamp hostname ns]}
   ;; Any extra appender-specific opts:
   & [{:keys [nofonts?] :as appender-fmt-output-opts}]]
  ;; <timestamp> <hostname> <LEVEL> [<ns>] - <message> <throwable>
  (format "%s [%s] - %s%s"
          (-> level name clojure.string/upper-case) ns (or message "")
          (or (timbre/stacktrace throwable "\n" (when nofonts? {})) "")))

(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:shared-appender-config :spit-filename] "albatross.log")
(timbre/merge-config! {:fmt-output-fn logger})

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
    (db/update-torrent! (assoc (db/find-or-create-by-bytes decoded-torrent) :state :seedbox))
    (seedbox/send-to seedbox decoded-torrent)
    "OK"))

(defn torrent-by-hash [params]
  (->
   (:hash params)
   (db/find-by-hash)
   (:bytes)
   (java.io.ByteArrayInputStream.)))

(defn render-layout [inner]
  (clojure.string/join (layout/layout inner)))

(defn render-partial [inner]
  (str/join (net.cgrand.enlive-html/emit* inner)))

(defn app-routes [provider seedbox]
  (try
    (routes
     ;; HTML
     ;; (GET "/shows/new" [] (render-layout (myshows/new)))
     ;; (POST "/shows/choose" {params :params} (render-partial (myshows/choose params)))
     ;; (POST "/shows/create" {params :params} (render-layout (myshows/create params)))
     ;; (GET "/shows/:id" [id] (render-layout (myshows/show id)))
     ;; (POST "/shows/:id/change" {params :params} (render-layout (myshows/change)))
     ;; (POST "/shows/:id/destroy" {params :params} (render-layout (myshows/destroy)))

     ;; (GET "/shows" [] (render-layout (myshows/index)))
     ;; (GET "/" [] (render-layout (myshows/index)))

     ;; API
     (POST "/search" {params :params} (provider/search-show provider params))
     (POST "/send_torrent" {params :params} (send-torrent-from-post seedbox params))
     (GET "/torrents/:id" {params :params} (torrent-by-hash params))
     (GET "/rss" request (provider/fetch-rss provider))

     (route/resources "/")
     (route/not-found "Not Found"))
    (catch Exception e
      (info e "Caught exception in routes"))))

(alter-var-root #'*out* (constantly *out*))

(def my-site-defaults
  (dissoc site-defaults :security))

(defn app [provider seedbox]
  (wrap-defaults (app-routes provider seedbox) my-site-defaults))

(defrecord HTTPServer [port server provider seedbox]
  component/Lifecycle
  (start [this]
    (info "Starting HTTP server")
    (if-not server
      (let [s (server/serve (app provider seedbox)
                            {:port port
                             :open-browser? false})]
        (info "created jetty server")
        (assoc this :server s))
      this))

  (stop [this]
    (info "Stopping HTTP server:")
    (when server (.stop server))
    (assoc this :server nil)))

(defn create-http-server [{port :port}]
  (map->HTTPServer {:port port}))
