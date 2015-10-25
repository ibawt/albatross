(defproject albatross "0.1.0-SNAPSHOT"
  :description "A web application to proxy sickbeard to a seedbox + torrent provider"
  :url "https://github.com/ibawt/albatross"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :uberjar-name "albatross-standalone.jar"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-time "0.11.0"]
                 [com.stuartsierra/component "0.3.0"]
                 [compojure "1.4.0"]
                 [environ "1.0.1"]
                 [bencode "0.2.5"]
                 [commons-codec "1.10"]
                 [ring/ring-json "0.4.0"]
                 [enlive "1.1.6"]
                 [cheshire "5.5.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [com.taoensso/timbre "4.1.4"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]
                 [korma "0.4.2"]
                 [clj-http "2.0.0"]
                 [ring-server "0.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [log4j "1.2.17" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [commons-daemon "1.0.15"]
                 [necessary-evil "2.0.0"]]

  :global-vars {*print-length* 2048
                *print-level* 7}

  :min-lein-version "2.3.0"

  :main albatross.core

  :source-paths ["src"]

  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]
                                  [clj-http-fake "1.0.1"]
                                  [org.clojure/tools.namespace "0.2.10"]]

                   :source-paths ["dev"]}
             :uberjar {:aot :all}})
