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
  [number-of-series]

  [:tr.series-row]
  (clone-for [i (range number-of-series)]
             [:td.series-name]
             (content "fdsfds")))
