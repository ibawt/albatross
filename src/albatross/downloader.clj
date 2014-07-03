(ns albatross.downloader
  (:require [clj-http.client :as http]
            [clojure.core.async :refer [go <! >! <!! chan unique thread]]
            [taoensso.timbre :as timbre]
            [albatross.torrent :as torrent]
            [albatross.torrentdb :as db]
            [clojure.java.io :as io]
            [environ.core :refer :all]
            [clojure.string :refer [join]]
            [clojure.java.shell :refer [sh]]))

(timbre/refer-timbre)
(def home-dir (.getAbsolutePath ^java.io.File (clojure.java.io/file (System/getProperty "user.home") "Torrents")))
(def download-dir [home-dir "downloads"])

(def download-channel (unique (chan 20)))

(def remote-base-url
  (env :rtorrents-download-url))

(def credentials
  [(env :rtorrent-username)
   (env :rtorrent-password)])

(defn get-download-dir [torrent]
  (conj download-dir (:name torrent)))

(defn create-dir [torrent]
  (let [dir ^java.io.File (apply io/file (get-download-dir torrent))]
    (when-not (.exists dir)
      (.mkdir dir))))

;;; NEEDS TO BE NOT TERRIBLE
;;; should handle partially downloaded better
(defn download-file [filename torrent size]
  (info "Downloading " filename " of " (:name torrent))
  (let [file ^java.io.File (apply io/file (conj (get-download-dir torrent) filename))]
    (when (< (.getTotalSpace file) size)
      (with-open [out (io/output-stream file)]
        (io/copy (:body (http/get (str remote-base-url (:name torrent) "/" filename)
                                  {:as :stream
                                   :basic-auth credentials
                                   :insecure? true
                                   })) out :buffer-size (* 1024 1024))))))

(defn fetch-multi [t]
  (create-dir t)
  ; clean up
  (doseq [file (:files t)]
    (let [path (filter #(> (count %1) 0) (get file "path"))]
      (when (> (count path) 1)
        (let [dir ^java.io.File (apply io/file
                         (concat (get-download-dir t)
                                 (butlast path)))]
          (when-not (.exists dir)
            (.mkdir dir))))
      (download-file (join "/" path) t (get file "length"))))
  (db/update-torrent (assoc t :state :done)))

(defn fetch-single [t]
  (create-dir t)
  (with-open [out (io/output-stream (apply io/file (conj (get-download-dir t) (:name t))))]
    (io/copy (:body (http/get (str remote-base-url (:name t))
                              {:as :stream
                               :basic-auth credentials
                               :insecure? true})) out :buffer-size (* 1024 1024))))

(defn notify-sickbeard [t]
  (try
    ; we shouldn't assume sickbeard is local to our box
    (http/get "http://127.0.0.1:8081/home/postprocess/processEpisode"
              { :query-params {:dir (join "/" (get-download-dir t))
                               :quiet "1"}})
    (catch Exception e)))

(defn fetch-torrent [t]
  (try
    (if (not (nil? (:files t)))
      (fetch-multi t)
      (fetch-single t))
    true
    (catch Exception e
      (error e)
      false)))

(defn- get-rar-files [t]
  (filter #(.endsWith (join "/" (get %1 "path")) ".rar") (:files t)))

(defn unpack-torrent [t]
  (doseq [file (get-rar-files t)]
    (sh "unrar" "x" "-y"  (join "/" (filter #(> (count %) 0) (get file "path"))) :dir (join "/" (get-download-dir t)))))

(def keep-downloading (atom true))

(defn downloader-job []
  (thread
    (while @keep-downloading
      (let [t-hash (<!! download-channel)
            t (get @db/db t-hash)]
        (when-not (= (:state t) :downloaded)
          (info "Got " (:name t) " to try and download!")
          (if (fetch-torrent t)
            (do (info "Fetched torrent: " (:name t))
                (db/update-torrent (assoc t :state :downloaded))
                (unpack-torrent t)
                (notify-sickbeard t))
            (warn "Torrent fetch failed: " (:name t))))))))
