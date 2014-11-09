(ns albatross.core
  (:import [org.apache.commons.daemon Daemon DaemonContext])
  (:gen-class
   :implements [org.apache.commons.daemon.Daemon])
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

(def home-dir
  (or (env :albatross-home)
      (.getAbsolutePath (clojure.java.io/file (System/getProperty "user.home") "Torrents"))))

(def db-file
  (.getAbsolutePath (clojure.java.io/file home-dir "albatross.db")))

(def config
  {:home-dir home-dir
   :port 3000
   :remote-base-url (env :rtorrents-download-url)
   :rtorrent {:username (env :rtorrent-username)
              :password (env :rtorrent-password)
              :endpoint-url (env :rtorrents-xmlrpc-url)}
   :iptorrents {:username (env :iptorrents-username)
                :password (env :iptorrents-password)
                :rss-url (env :iptorrents-rss-url)
                :pass (env :iptorrents-torrent-pass)}
   :db-file db-file})

(defrecord AlbatrossSystem [config downloader poller]
  component/Lifecycle
  (start [this]
    (info "Albatross system start...")
    (component/start-system this components))
  (stop [this]
    (info "Albatross system stop...")
    (component/stop-system this components)))

(defn albatross-system [config]
  (map->AlbatrossSystem {:config config
                         :db (albatross.db/create-database config)
                         :seedbox (seedbox/create-seedbox config)
                         :downloader (component/using (downloader/create-downloader config)
                                                      [:db])
                         :poller (component/using
                                  (albatross.poller/create-poller)
                                  [:db :downloader :seedbox])
                         :provider (albatross.provider/create-provider config)
                         :app (component/using
                               (handler/create-http-server config)
                               [:provider :seedbox])}))

(def albatross-app nil)

(defn -main [& args]
  (component/start
   (albatross-system config)))

;; Daemon implementation
(defn -init [this ^DaemonContext context]
  (info (.getArguments context))
  (alter-var-root #'albatross-app (constantly (albatross-system config))))

(defn -start [this]
  (future (alter-var-root #'albatross-app component/start)))

(defn -stop [this]
  (alter-var-root #'albatross-app
                  (fn [s] (when s (component/stop s)))))
