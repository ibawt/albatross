(ns albatross.myshows
  (:require [albatross.tvdb :as tvdb]
            [korma.core :refer :all]
            [taoensso.timbre :as timbre]
            ))

(timbre/refer-timbre)

(defentity myshows
  (prepare tvdb/underscoreize-keys)
  (transform tvdb/deunderscore-keys)
  (has-one tvdb/series))

(defn index
  "returns the collection of myshows"
  []
  (select myshows))

(defn search
  "searches tvdb for your show"
  [params]
  (tvdb/search-series (str (:q params) "*")))

(defn select-show
  "selects the show"
  [params])

(defn create
  "creates teh show"
  [params])
