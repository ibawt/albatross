(defproject albatross "0.1.0-SNAPSHOT"
  :description "A web application to proxy sickbeard to a seedbox + torrent provider"
  :url "https://github.com/ibawt/albatross"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}

  :uberjar-name "albatross-standalone.jar"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-time "0.8.0"]
                 [compojure "1.2.1"]
                 [environ "1.0.0"]
                 [bencode "0.2.5"]
                 [commons-codec "1.9"]
                 [ring/ring-json "0.3.1"]
                 [enlive "1.1.5"]
                 [cheshire "5.3.1"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [com.taoensso/timbre "3.3.1"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.xerial/sqlite-jdbc "3.8.7"]
                 [korma "0.4.0"]
                 [clj-http "1.0.1"]
                 [ring-server "0.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [com.stuartsierra/component "0.2.2"]
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
                                  [clj-http-fake "0.7.8"]
                                  [org.clojure/tools.namespace "0.2.7"]]

                   :source-paths ["dev"]}
             :uberjar {:aot :all}})
