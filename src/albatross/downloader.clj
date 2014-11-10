(ns albatross.downloader
  (:require [clj-http.client :as http]
            [taoensso.timbre :as timbre]
            [albatross.torrentdb :as db]
            [clojure.java.io :as io]
            [environ.core :refer :all]
            [clojure.string :refer [join]]
            [clojure.java.shell :refer [sh]]
            [clojure.core.async :refer
             [<!! >!! chan unique thread close! alt!!]]
            [com.stuartsierra.component :as component]))

(timbre/refer-timbre)

(def ^:private BUFFER_SIZE (* 1024 1024))

(defn- get-download-dir [this torrent]
  (conj (:dir this) (:name torrent)))

(defn- create-dir [this torrent]
  (let [dir ^java.io.File (apply io/file (get-download-dir this torrent))]
    (when-not (.exists dir)
      (.mkdir dir))))

;;; NEEDS TO BE NOT TERRIBLE
;;; should handle partially downloaded better
(defn- download-file [this filename torrent size]
  (infof "downloading: %s" filename)
  (let [file ^java.io.File (apply io/file (conj (get-download-dir this torrent) filename))]
    (when (< (.getTotalSpace file) size)
      (with-open [out (io/output-stream file)]
        (io/copy (:body (http/get (str (:remote-base-url this) (:name torrent) "/" filename)
                                  {:as :stream
                                   :basic-auth (:credentials this)
                                   :insecure? true})) out :buffer-size BUFFER_SIZE)))))


(defn- fetch-multi [this t]
  (create-dir this t)
  ; clean up
  (doseq [file (:files t)]
    (let [path (filter #(pos? (count %1)) (get file :path))]
      (when (pos? (count path))
        (let [dir ^java.io.File (apply io/file
                         (concat (get-download-dir this t)
                                 (butlast path)))]
          (when-not (.exists dir)
            (.mkdir dir))))
      (download-file this (join "/" path) t (get file :length))))
  (db/update-torrent! (assoc t :state :done)))

(defn- fetch-single [this t]
  (create-dir this t)
  (with-open [out (io/output-stream
                   (apply io/file (conj (get-download-dir this t) (:name t))))]
    (io/copy (:body (http/get (str (:remote-base-url this) (:name t))
                              {:as :stream
                               :basic-auth (:credentials this)
                               :insecure? true}))
             out :buffer-size BUFFER_SIZE)))

(defn- notify-sickbeard [this t]
  (try
    ; FIXME we shouldn't assume sickbeard is local to our box
    (http/get "http://127.0.0.1:8081/home/postprocess/processEpisode"
              { :query-params {:dir (join "/" (get-download-dir this t))
                               :quiet "1"}})
    (catch Exception e)))

(defn- fetch-torrent [this t]
  (try
    (if-not (nil? (:files t))
      (fetch-multi this t)
      (fetch-single this t))
    true
    (catch Exception e
      (error e)
      false)))

(defn- get-rar-files [t]
  (filter #(.endsWith (join "/" (get %1 :path)) ".rar") (:files t)))

(defn- unpack-torrent [this t]
  (doseq [file (get-rar-files t)]
    (let [r  (sh "unrar" "x" "-y"  (join "/" (filter #(pos? (count %)) (get file :path))) :dir (join "/" (get-download-dir this t)))]
      (if (= (:exit r) 0)
        (do (info "unarchive success!")
            (db/update-torrent! (assoc t :state :complete)))
        (errorf "Error un-archiving: %s output: %s" (:name t) (:out r))))))

(defn- do-download [this t]
  (infof "[%d] %s is ready for download" (:id t) (:name t))
  (try
    (when (= (:state t) :ready-to-download)
      (if (fetch-torrent this t)
        (do
          (db/update-torrent! (assoc t :state :downloaded))
          (unpack-torrent this t)
          (notify-sickbeard this t))))
    (catch Exception e
      (warn e))))

(defn- downloader-job [this job-id]
  (info "[Job]: " job-id " started")
  (thread
    (loop []
      (info "Waiting for download...")
      (when-let [t (<!! (:download-queue this))]
        (do-download this t)
        (recur)))
    (info "[Job]: " job-id " stopped")))

(defn- populate-download-queue [queue]
  "grabs ready to download items from the db for app init, otherwise things come from the poller"
  (thread
    (doseq [t (db/by-state :ready-to-download)]
      (infof (:name t) "is ready to download")
      (>!! queue t))))

(defn- unpack-downloads[this]
  "looks for things that should be unpacked"
  (thread
    (doseq [t (db/by-state :downloaded)]
      (unpack-torrent this t)
      (db/update-torrent! (assoc t :state :complete)))))

(defrecord Downloader [dir download-queue jobs credentials remote-base-url
                       sickbeard-post-process-url concurrent-downloads]
  component/Lifecycle

  (start [this]
    (if-not download-queue
      ;; FIXME code is ugly here
      (let [num-jobs (:concurrent-downloads this)
            t (assoc this :download-queue (unique (chan num-jobs)))]
        ;; FIXME these next two should be serial I think
        (populate-download-queue (:download-queue t))
        (unpack-downloads t)
        (assoc t :jobs (into [] (for [n (range num-jobs)]
                                  (downloader-job t n)))))
      this))

  (stop [this]
    (if download-queue
      (do
        (close! download-queue)
        (assoc this :download-queue nil))
      this)))

(def ^:private defaults
  {:concurrent-downloads 1
   :sickbeard-post-process-url
   "http://127.0.0.1:8081/home/postprocess/processEpisode"})

(defn create-downloader [config]
  (let [c (merge defaults config)]
    (map->Downloader
     {:dir [(:home-dir c) "downloads"]
      :credentials [(get-in c [:rtorrent :username]) (get-in c [:rtorrent :password])]
      :remote-base-url (:remote-base-url c)
      :sickbeard-post-process-url (:sickbeard-post-process-url c)
      :concurrent-downloads (:concurrent-downloads c)})))
