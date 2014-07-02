(defproject albatross "0.1.0-SNAPSHOT"
  :description "A web application to proxy sickbeard to a seedbox + torrent provider"
  :url "https://github.com/ibawt/albatross"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [necessary-evil "2.0.0"]
                 [environ "0.5.0"]
                 [bencode "0.2.5"]
                 [commons-codec "1.9"]
                 [ring/ring-json "0.3.1"]
                 [enlive "1.1.5"]
                 [cheshire "5.3.1"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [com.taoensso/timbre "3.2.1"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [korma "0.3.2"]
                 [clj-http "0.9.2"]
                 [liberator "0.11.0"]
                 [ring/ring-defaults "0.1.0"]]

  :min-lein-version "2.3.0"

  :source-paths ["src"]

  :plugins [[lein-ring "0.8.10"]
            [cider/cider-nrepl "0.7.0-SNAPSHOT"]
            [lein-environ "0.5.0"]]

  :ring {:handler albatross.handler/app
         :auto-reload? true
         :init albatross.handler/init
         :destroy albatross.handler/destroy
         :auto-refresh? true}

  :profiles { :dev {:injections [(require 'pjstadig.humane-test-output)
                                 (pjstadig.humane-test-output/activate!)
                                 (require '[taoensso.timbre :as timbre])
                                 (timbre/refer-timbre)]

                    :dependencies [[javax.servlet/servlet-api "2.5"]
                                   [ring-mock "0.1.5"]
                                   [ring-server "0.3.1"]
                                   [pjstadig/humane-test-output "0.6.0"]]}})
