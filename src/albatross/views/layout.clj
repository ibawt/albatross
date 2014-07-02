(ns albatross.views.layout
  (:require [net.cgrand.enlive-html :refer :all]))

(deftemplate layout
  "albatross/views/layout.html"
  [body & js]

  [:div#app]
  (content body)

  [:script#js-code]
  (content js))

(defsnippet home
  "albatross/views/home.html" [:div#home]
  [shows]

  [:tr.series-row]
  (clone-for [show shows]
             [:td.series-name]
             (content (:series-id show))))


(defsnippet add-show
  "albatross/views/add-show.html" [:div#home]
  []

  [:p]
  (content ""))

(defsnippet search-show
  "albatross/views/search-show.html" [:div#home]
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
