(ns albatross.tvdb
  (:require [clj-http.client :as http]
            [taoensso.timbre :as timbre]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :refer :all]
            [clojure.java.io :as io]
            [korma.core :refer :all]
            [clojure.string :as string]
            [albatross.db :as db]
            ))

(timbre/refer-timbre)

(declare series episodes)

(defn filter-episodes [x]
  (filter #(= (:tag %) :Episode) (:content x)))

(defn underscoreize [k]
  (keyword (clojure.string/replace (name k) "-" "_")))

(defn deunderscorize [k]
  (keyword (clojure.string/replace (name k) "_" "-")))

(defn underscoreize-keys [m]
  (into {} (for [[k v] m] [(underscoreize k) v])))

(defn deunderscore-keys [m]
  (into {} (for [[k v] m] [(deunderscorize k) v])))

(defn join-aliases [m]
  (if (:aliases m)
    (assoc m :aliases (clojure.string/join "|" (:aliases m)))
    m))

(defn unjoin-aliases [m]
  (if (:aliases m)
    (assoc m :aliases (clojure.string/split (:aliases m) #"\|"))
    m))

(defentity series
  (has-many episodes)
  (prepare #(-> % (underscoreize-keys) (join-aliases)))
  (transform #(-> % (deunderscore-keys) (unjoin-aliases)))
)

(defentity episodes
  (belongs-to series {:fk :series-id})
  (prepare #(-> % (underscoreize-keys) (join-aliases)))
  (transform #(-> % (deunderscore-keys) (unjoin-aliases)))
  )

(def ^:private base-url "http://thetvdb.com/api/")

;; lololol sick beard keeps it's api key in it's git repo
(def api-key "9DAF49C96CBF8DAC")

(def mirrors-url (str base-url api-key "/mirrors.xml"))

(defn get-xml [url]
  (->
   (http/get url {:as :stream})
   (:body)
   (xml/parse)
   (zip/xml-zip)))

(defn parse-mirror [m]
  {:id (read-string (first (xml-> m :id text)))
   :mirrorpath (first (xml-> m :mirrorpath text))
   :typemask (read-string (first (xml-> m :typemask text)))})

(def mirrors
  (map parse-mirror
       (xml-> (get-xml mirrors-url) :Mirror)))

(def mirror-url
  (:mirrorpath (first mirrors)))


(defn api-url [& api]
  (string/join "/" (concat [mirror-url "api"] api)))

(defn search-series [s]
  (->
   (http/get (api-url "GetSeries.php")
             {:query-params {:seriesname s
                             :language "en"}
              :as :stream})
   (:body)
   (xml/parse)
   (zip/xml-zip)))

(defn get-current-server-time []
  (->
   (http/get (api-url "Updates.php")
             {:query-params {:type "none"}
              :as :stream})
   (:body)
   (xml/parse)
   (zip/xml-zip)
   (#(xml-> % :Time text))
   (first)
   (read-string)))


(defn to-number [x]
  (if (= x "")
    nil
    (read-string x)))

(defn parse-series [x]
  {:tvdb-id (read-string (first (xml-> x :Series :seriesid text)))
   :language (first (xml-> x :Series :language text))
   :name (first (xml-> x :Series :SeriesName text))
   :aliases (vec (xml-> x :Series :AliasNames text))
   :banner (first (xml-> x :Series :banner text))
   :overview (first (xml-> x :Series :Overview text))
   :first-aired (first (xml-> x :Series :FirstAired text))
   :network (first (xml-> x :Series :Network text))
   :imdb-id (first (xml-> x :Series :IMDB_ID text))
   :zap2it-id (first (xml-> x :Series :zap2it_id  text))})

(defn parse-episode [x]
  {:tvdb-id (to-number (first (xml-> x :id text)))
   :name (first (xml-> x :EpisodeName text))
   :number (to-number (first (xml-> x :EpisodeNumber text)))
   :first-aired (first (xml-> x :FirstAired text))
   :imdb-id (first (xml-> x :IMDB_ID text))
   :overview (first (xml-> x :Overview text))
   :season (to-number (first (xml-> x :SeasonNumber text)))
   :production-code (first (xml-> x :ProductionCode text))
   :rating (to-number (first (xml-> x :Rating text)))
   :rating-count (to-number (first (xml-> x :RatingCount text)))
   :series-id (to-number (first (xml-> x :seriesid text)))
   :season-id (to-number (first (xml-> x :seasonid text)))
   :filename (first (xml-> x :filename text))
   :thumb-width (to-number (first (xml-> x :thumb_width text)))
   :thumb-height (to-number (first (xml-> x :thumb_height text)))
   }
  )

(defn fetch-show-data [show]
  (->
   (http/get (api-url api-key "series" (:tvdb-id show) "all") {:as :stream})
   (:body)
   (xml/parse)))

(defn save-to-db [show]
  (insert series show))

(def ^:dynamic *last-server-time* (atom (get-current-server-time)))
