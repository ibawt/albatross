(ns albatross.providers.btn
  (:require [clj-http.client :as http]
            [albatross.torrentdb :as db]
            [cheshire.core :refer :all]
            [taoensso.timbre :as timbre]
            [environ.core :refer :all]))

(timbre/refer-timbre)

(def ^:private btn-url "http://api.btnapps.net")

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn- to-number [n]
  (if (string? n) (Integer/parseInt n) n))

(defn- create-query [show-params]
  {:series (:show_name show-params)
   :origin "scene"
   :name (format "S%02dE%02d" (to-number (:season show-params))  (to-number (:episode show-params)))})

(defn- json-rpc-body [api-key method params]
  (generate-string {:method method :params [api-key params 25]
                    :id 1}))

(defn- call [body]
  (:body (http/post btn-url {:body body :content-type "application/json-rpc" :as :json})))

(defn- search-results [config params]
  (get-in  (call (json-rpc-body (:api-key config) "getTorrents" (create-query params))) [:result :torrents]))

(defn- make-link [[id show]]
  (str "http://localhost:3000/torrents/" (:ReleaseName show) ".torrent?hash=" (clojure.string/lower-case (:InfoHash show))))

(defn- fetch-torrent [api-key show]
  (db/find-or-create-by-bytes (:body (http/get (:DownloadURL show) {:as :byte-array}))))

(defn search-show [config params]
  (let [results (search-results config params)]
    (doseq [[k t] results]
      (info "fetching " (:ReleaseName t))
      (fetch-torrent (:api-key config) t))
    (map make-link results)))


(def ^:private config
  {:search-show search-show
   :name "Broadcast The Net"
   :backlog? false
   :magnet false})

(defn create[{btn :btn}]
  (assoc config :api-key (:api-key btn)))
