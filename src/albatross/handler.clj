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
						[taoensso.timbre :as timbre]
						[ring.server.standalone :as server]
						[clojure.java.io :as io]
						[albatross.myshows :as myshows]))

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
	[request]
	(let [decoded-torrent (ring.util.codec/base64-decode (:file request))]
		(db/update-torrent (assoc (torrent/find-or-create-by-bytes decoded-torrent) :state :seedbox))
		(seedbox/send-to decoded-torrent)
		"OK"))

(defn init []
	(info "Albatross -- INIT")
	(db/load-or-create)
	(poller/start-poller)
	(downloader/downloader-job))

(defn destroy []
	(info "Albatross -- destroy"))

(defn torrent-by-hash [params]
	(java.io.ByteArrayInputStream. (torrent/torrent->bytes (db/hash->torrent (:hash params)))))

(defn render-layout [inner]
	(apply str (layout/layout inner)))

(defroutes app-routes
	;; HTML
	(GET "/shows/new" [] (render-layout (myshows/new)))
	(POST "/shows/choose" {params :params} (render-layout (myshows/choose params)))
	(POST "/shows/create" {params :params} (render-layout (myshows/create params)))
	(GET "/shows/:id" {params :params} (render-layout (myshows/show params)))
	(POST "/shows/:id/change" {params :params} (render-layout (myshows/change)))
	(POST "/shows/:id/destroy" {params :params} (render-layout (myshows/destroy)))

	(GET "/shows" [] (render-layout (myshows/index)))
	(GET "/" [] (render-layout (myshows/index)))

	;; API
	(POST "/search" {params :params} (provider/search-show params))
	(POST "/send_torrent" {params :params} (send-torrent-from-post params))
	(GET "/torrents/:id" {params :params} (torrent-by-hash params))
	(GET "/rss" request (provider/fetch-rss))

	(route/resources "/")
	(route/not-found "Not Found"))

(alter-var-root #'*out* (constantly *out*))

(def my-site-defaults
	(dissoc site-defaults :security))

(def app
	(wrap-defaults app-routes my-site-defaults))

(defn standalone []
	(server/serve app {:stacktraces? false}))
