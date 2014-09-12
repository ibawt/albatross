(ns albatross.core
  (:require
   [com.stuartsierra.component :as component]
   [environ.core :refer :all]
   [taoensso.timbre :as timbre]
   [albatross.downloader :as downloader]
   [albatross.handler :as handler]
   [albatross.poller :as poller]
   [albatross.seedbox :as seedbox]))

(timbre/refer-timbre)

(def components
  [:downloader :poller :provider :seedbox :app :db])

(def config
  {:home-dir (.getAbsolutePath (clojure.java.io/file
                                (System/getProperty "user.home") "Torrents"))
   :port 3000
   :rtorrent {:username (env :rtorrent-username)
              :password (env :rtorrent-password)
              :endpoint-url (env :rtorrents-xmlrpc-url)}
   :iptorrents {:username (env :iptorrents-username)
                :password (env :iptorrents-password)
                :rss-url (env :iptorrents-rss-url)
                :pass (env :iptorrents-torrent-pass)}
   :db-file "abatross.db"})

(defrecord AlbatrossSystem [config downloader torrent-db poller]
  component/Lifecycle
  (start [this]
    (info "AlbatrossSystem starting")
    (component/start-system this components))
  (stop [this]
    (info "AlbatrossSystem stopping")
    (component/stop-system this components)))

(defn albatross-system [config]
  (map->AlbatrossSystem {:config config
                         :db (albatross.db/create-database config)
                         :seedbox (seedbox/create-seedbox config)
                         :downloader (downloader/create-downloader config)
                         :poller (component/using
                                  (albatross.poller/create-poller)
                                  [:downloader])
                         :provider (albatross.provider/create-provider config)
                         :app (component/using
                               (handler/create-http-server config)
                               [:provider :seedbox])}))
(defn -main [& args]
  (component/start
   (albatross-system config)))
