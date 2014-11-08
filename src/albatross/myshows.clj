(ns albatross.myshows
  (:require [albatross.tvdb :as tvdb]
            [korma.core :refer :all]
            [taoensso.timbre :as timbre]
            ;; [albatross.views.myshows.core :as view]
            ))

(timbre/refer-timbre)

(defentity myshows
  (prepare tvdb/underscoreize-keys)
  (transform tvdb/deunderscore-keys))

(defentity myepisodes
  (prepare tvdb/underscoreize-keys)
  (transform tvdb/deunderscore-keys))

(defn find-myshow [tvdb-id]
  (first (select myshows (where (= :series_id tvdb-id)))))

(defn find-myshow-id [id]
  (first (select myshows (where (= :id id)))))

(defn get-series []
  (select myshows (fields (raw "myshows.id as myshow_id") (raw "series.*")) (join tvdb/series (= :series_id :series.tvdb_id))))

(defn episodes-for-myshow [myshow]
  (let [series (tvdb/tvdb-id->series (:tvdb-id myshow))
        episodes (tvdb/episodes-for-series)]))

(defn create-myepisodes [tvdb-id]
  (let [myshow (find-myshow tvdb-id)
        epis (tvdb/episodes-for-series {:tvdb-id tvdb-id})]
    (map #(insert myepisodes (values [{:myshow-id (:id myshow)
                                       :episode-id (:tvdb-id %1)
                                       :state "not-present"
                                       } ])) epis)))

;; (defn index
;;   []
;;   (view/index (get-series)))

;; (defn new
;;   []
;;   (view/new))
;; ;;
(defn filter-search-results [results]
  (filter :first-aired (filter :imdb-id results)))

;; (defn choose
;;   [params]
;;   (view/choose (filter-search-results (tvdb/search-series (str "*" (:q params) "*")))))

(defn create
  [{db-id :show}]
  (do
    (tvdb/populate-series db-id)
    (doall (insert myshows (values [{:series-id db-id}])))
    (create-myepisodes db-id)))

(defn change [params])

(defn get-episodes [myshow-id]
  (select myepisodes
          (fields (raw "myepisodes.filename as myepisodes_filename")
                  (raw "myepisodes.id as myepisode_id")
                  (raw "myepisodes.state as myepisode_state")
                  (raw "episodes.*"))
          (join tvdb/episodes (= :episode_id :episodes.tvdb_id))
          (where (= :myshow_id myshow-id))))

(defn group-and-sort [id]
  (reverse (map #(reverse (sort-by :number %))
                (partition-by :season (get-episodes id)))))

(defn show [id]
  (let [myshow (find-myshow-id id)
        series (tvdb/tvdb-id->series (:series-id myshow))
        epis (group-and-sort id)]

    ;; (view/show series myshow epis)
    ))

(defn destroy [params])
