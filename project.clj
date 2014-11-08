(defproject albatross "0.1.0-SNAPSHOT"
  :description "A web application to proxy sickbeard to a seedbox + torrent provider"
  :url "https://github.com/ibawt/albatross"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [necessary-evil "2.0.0"]
                 [environ "1.0.0"]
                 [bencode "0.2.5"]
                 [commons-codec "1.9"]
                 [ring/ring-json "0.3.1"]
                 [enlive "1.1.5"]
                 [cheshire "5.3.1"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [com.taoensso/timbre "3.2.1"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [korma "0.4.0"]
                 [clj-http "1.0.0"]
                 [ring-server "0.3.1"]
                 [ring/ring-defaults "0.1.1"]
                 [com.stuartsierra/component "0.2.1"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [org.apache.commons/commons-daemon "1.0.9"]]

  :global-vars {*print-length* 2048
                *print-level* 7}

  :min-lein-version "2.3.0"

  :main albatross.core

  :source-paths ["src"]

  :plugins [[lein-ring "0.8.10"]
            [cider/cider-nrepl "0.8.0-SNAPSHOT"]
            [lein-environ "0.5.0"]]

  :profiles {:dev {:injections [(require 'pjstadig.humane-test-output)
                                 (pjstadig.humane-test-output/activate!)]

                    :dependencies [[javax.servlet/servlet-api "2.5"]
                                   [ring-mock "0.1.5"]
                                   [clj-http-fake "0.7.8"]
                                   [ring-server "0.3.1"]
                                   [org.clojure/tools.namespace "0.2.5"]
                                   [pjstadig/humane-test-output "0.6.0"]]

                    :source-paths ["dev"]}
             :uberjar {:aot :all}})
