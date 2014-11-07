(ns albatross.core-test
  (:require [clojure.test :refer :all]
            [albatross.core :refer :all]
            [com.stuartsierra.component :as component]
            [albatross.utils :as utils]))

(def tmp-dir (.getAbsolutePath (clojure.java.io/file (System/getProperty "java.io.tmpdir") "Torrents")))

(defn create-test-dir []
  (.mkdir (clojure.java.io/file tmp-dir)))

(defn destroy-test-dir []
  (utils/delete-file-recursively tmp-dir))

(defn wrap-tests [fn]
  (create-test-dir)
  (fn)
  (destroy-test-dir))

(def test-config
  {:db-file "albatross-test.db"
   :home-dir tmp-dir
   :port 6000
   :rtorrent {:username "foo"
              :password "bar"
              :endpoint-url "http://foobar.com/foobar"}
   :iptorrents {:username "foo"
                :password "bar"
                :rss-url "http://foobar.com/rss"
                :pass "1337"}})

(use-fixtures :each wrap-tests)

(deftest start-and-stop
  (testing "start multiple times")
  (testing "stop and start")
  (testing "stop multiple times"))
