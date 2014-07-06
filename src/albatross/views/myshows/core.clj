(ns albatross.views.myshows.core
  (:require [net.cgrand.enlive-html :refer :all]
            [net.cgrand.reload :refer :all]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(net.cgrand.reload/auto-reload *ns*)

(defn show-link [show]
  (str "/shows/" (:myshow-id show)))

(defn show-banner [show]
  (str "http://thetvdb.com/banners/" (:banner show)))

(defsnippet index
  "albatross/views/myshows/index.html" [:div#home]
  [shows]

  [:tr.series-row]
  (clone-for [show shows]
             [:td.series-name :a]
             (set-attr :href (show-link show))

             [:td.series-name :img]
             (set-attr :src (show-banner show))))

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

(defsnippet show
  "albatross/views/myshows/show.html" [:div#home]
  [series myshow episodes]

  [:div.season]
  (clone-for [[idx season] (map-indexed vector episodes)]

             [:div.season-header :h2]
             (content (str "Season: " (- (count (butlast episodes)) idx)))

             [:tr.episode-row]
             (clone-for [epi season]
                        [:td.number]
                        (content (str (:number epi)))

                        [:td.name]
                        (content (:name epi))

                        [:td.air-date]
                        (content (:first-aired epi))

                        [:td.status]
                        (content (:myepisode-state epi))

                        [:a.search-show-link]
                        (set-attr :href (str "/shows/" (:id myshow) "/episodes/" (:myepisode-id epi) "/search" ))
                        )
             )
  )
