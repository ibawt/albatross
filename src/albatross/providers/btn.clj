(ns albatross.providers.btn
  (:require [clj-http.client :as http]
            [cheshire.core :refer :all]
            [taoensso.timbre :as timbre]
            [environ.core :refer :all]))

(def ^:private btn-url "http://api.btnapps.net")

(defn- make-query [api-key]
  (generate-string {:method "getTorrents" :params [api-key {}]})
  )


(defn- send [body]
  (:body (http/post btn-url {:body body :content-type "application/json-rpc" :as :json})))

(defn- search-results [config params])

(defn search-show [config params]
  (-> (search-results config params)
      ;(parse-search-results)
      ))

(def ^:private config
  {:search-show search-show
   :name "Broadcast The Net"
   :backlog? true
   :magnet false})

(defn create[{btn :btn}]
  (assoc config :api-key (:api-key btn)))
