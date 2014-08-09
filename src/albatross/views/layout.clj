(ns albatross.views.layout
  (:require [net.cgrand.enlive-html :refer :all]
            [net.cgrand.reload :refer :all]
            ))

(net.cgrand.reload/auto-reload *ns*)
(deftemplate layout
  "albatross/views/layout.html"
  [body & js]

  [:div#app]
  (content body)

  [:script#js-code]
  (content js))
