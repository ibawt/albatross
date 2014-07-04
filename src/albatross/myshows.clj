(ns albatross.myshows
	(:require [albatross.tvdb :as tvdb]
						[korma.core :refer :all]
						[taoensso.timbre :as timbre]
						[albatross.views.myshows.core :as view]))

(timbre/refer-timbre)

(defentity myshows
	(prepare tvdb/underscoreize-keys)
	(transform tvdb/deunderscore-keys)
	(has-one tvdb/series))

(defn index
	[]
	(view/index (select myshows)))

(defn new
	[]
	(view/new))

(defn choose
	[params]
	(view/choose (filter #(some? (:first-aired %1)) (tvdb/search-series (str (:q params) "*")))))

(defn create
	[params]
	(let [db-id (:show params)] (tvdb/fetch-show-data (:show params))
			 (insert myshows (values [{:series-id db-id}]))
			 ))

(defn change [params])

(defn show [params])

(defn destroy [params])
