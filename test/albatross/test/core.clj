(ns albatross.test.core
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
  {:home-dir tmp-dir
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
  (testing "start multiple times"
    (let [system (component/start (albatross-system test-config))]
      (is (= system (component/start system)))
      (component/stop system)))
  (testing "stop and start"
    (let [system (component/start (albatross-system test-config))]
      (component/stop system)
      (is true)))
  (testing "stop multiple times"
    (let [system (component/stop (component/start (albatross-system test-config)))]
      (is (= system (component/stop system))))))
