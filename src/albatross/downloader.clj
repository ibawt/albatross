(ns albatross.downloader
  (:require [clj-http.client :as http]
            [clojure.core.async :refer [go <! >! <!! chan unique thread]]
            [taoensso.timbre :as timbre]
            [albatross.torrentdb :as db]
            [clojure.java.io :as io]
            [environ.core :refer :all]
            [clojure.string :refer [join]]
            [clojure.java.shell :refer [sh]]
            [com.stuartsierra.component :as component]))

(timbre/refer-timbre)

(def concurrent-downloads 3)

(defn- get-download-dir [this torrent]
  (conj (:dir this) (:name torrent)))

(defn- create-dir [this torrent]
  (let [dir ^java.io.File (apply io/file (get-download-dir this torrent))]
    (when-not (.exists dir)
      (.mkdir dir))))

;;; NEEDS TO BE NOT TERRIBLE
;;; should handle partially downloaded better
(defn- download-file [this filename torrent size]
  (info "Downloading " filename " of " (:name torrent))
  (let [file ^java.io.File (apply io/file (conj (get-download-dir this torrent) filename))]
    (when (< (.getTotalSpace file) size)
      (with-open [out (io/output-stream file)]
        (io/copy (:body (http/get (str (:remote-base-url this) (:name torrent) "/" filename)
                                  {:as :stream
                                   :basic-auth (:credentials this)
                                   :insecure? true
                                   })) out :buffer-size (* 1024 1024))))))

(defn- fetch-multi [this t]
  (create-dir this t)
  ; clean up
  (doseq [file (:files t)]
    (let [path (filter #(pos? (count %1)) (get file "path"))]
      (when (pos? (count path))
        (let [dir ^java.io.File (apply io/file
                         (concat (get-download-dir this t)
                                 (butlast path)))]
          (when-not (.exists dir)
            (.mkdir dir))))
      (download-file this (join "/" path) t (get file "length"))))
  (db/update-torrent (assoc t :state :done)))

(defn- fetch-single [this t]
  (create-dir this t)
  (with-open [out (io/output-stream (apply io/file (conj (get-download-dir t) (:name t))))]
    (io/copy (:body (http/get (str (:remote-base-url this) (:name t))
                              {:as :stream
                               :basic-auth (:credentials this)
                               :insecure? true})) out :buffer-size (* 1024 1024))))

(defn- notify-sickbeard [this t]
  (try
    ; we shouldn't assume sickbeard is local to our box
    (http/get "http://127.0.0.1:8081/home/postprocess/processEpisode"
              { :query-params {:dir (join "/" (get-download-dir this t))
                               :quiet "1"}})
    (catch Exception e)))

(defn- fetch-torrent [t]
  (try
    (if-not (nil? (:files t))
      (fetch-multi t)
      (fetch-single t))
    true
    (catch Exception e
      (error e)
      false)))

(defn- get-rar-files [t]
  (filter #(.endsWith (join "/" (get %1 "path")) ".rar") (:files t)))

(defn- unpack-torrent [this t]
  (doseq [file (get-rar-files t)]
    (sh "unrar" "x" "-y"  (join "/" (filter #(pos? (count %)) (get file "path"))) :dir (join "/" (get-download-dir this t)))))

(defn downloader-job [this]
  (thread
    (while (:running this)
      (let [t-hash (<!! (:channel this))
            t (db/get-torrent (:torrent-db this) t-hash)]
        (when-not (= (:state t) :downloaded)
          (info "Got " (:name t) " to try and download!")
          (if (fetch-torrent t)
            (do (info "Fetched torrent: " (:name t))
                (db/update-torrent (assoc t :state :downloaded))
                (unpack-torrent t)
                (notify-sickbeard t))
            (warn "Torrent fetch failed: " (:name t))))))))

(defrecord Downloader [dir channel credentials remote-base-url running torrent-db]
  component/Lifecycle

  (start [this]
    (info "Starting Downloader")
    (when-not running
      (reset! running true)
      (downloader-job this))
    this)

  (stop [this]
    (info "Stopping downloader")
    (when running
      (reset! running false))
    this))

(defn create-downloader [config]
  (map->Downloader
   {:dir [(:home-dir config) "downloads"]
    :channel (unique (chan concurrent-downloads))
    :credentials [(:rtorrent-username config) (:rtorrent-password config)]
    :remote-base-url (:remote-base-url config)
    :running (atom false)}))
