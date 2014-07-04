(ns albatross.views.myshows.core
	(:require [net.cgrand.enlive-html :refer :all]
						[taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(defsnippet index
	"albatross/views/myshows/index.html" [:div#home]
	[shows]

	[:tr.series-row]
	(clone-for [show shows]
						 [:td.series-name]
						 (content (:series-id show))))


(defsnippet new
	"albatross/views/myshows/new.html" [:div#home]
	[]

	[:p]
	(content ""))

(defsnippet choose
	"albatross/views/myshows/choose.html" [:div#home]
	[results]

	[:tr.series-row]
	(clone-for [show results]
						 [:input.show-select]
						 (set-attr :value (:tvdb-id show))

						 [:td.series-name]
						 (content (:name show))

						 [:td.series-first-aired]
						 (content (:first-aired show))

						 [:img.series-image]
						 (when-let [banner (:banner show)]
							 (set-attr :src (str "http://thetvdb.com/banners/_cache/" banner)))

						 [:a.imdb-link]
						 (set-attr :href (str "http://www.imdb.com/title/" (:imdb-id show)))))
