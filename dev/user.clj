(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [com.stuartsierra.component :as component]
   [taoensso.timbre :as timbre]
   [albatross.downloader :as downloader]
   [albatross.core :as core]
   [albatross.torrentdb :as db]
   [albatross.seedbox :as seedbox]
   [environ.core :refer :all]
   [clj-http.client :as http]
   [korma.core :refer :all]))

(timbre/refer-timbre)

(def config core/config)

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root #'system
                  (constantly (core/albatross-system config))))

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root #'system component/start-system))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root #'system
   (fn [s] (when s (component/stop-system s)))))

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))

(defn torrents-by-id [& ids]
  (select db/torrents (where {:id [in ids]})))


;; (defn sync-torrents []
;;   (let [remote-torrents (seedbox/list-torrents (:seedbox system))]
;;     (map (fn [r]
;;            (let [mine (db/find-by-hash (:hash r))]
;;              (if mine
;;                (merge mine (create-local-torrent r))
;;                (create-local-torrent r))))
;;          remote-torrents)))
