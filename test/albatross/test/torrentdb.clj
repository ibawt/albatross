(ns albatross.torrentdb-test
  (:use clojure.test
        ring.mock.request
        albatross.torrentdb)
  (:require [albatross.utils :as utils]
            [albatross.db :as db]
            [clojure.java.io :as io]
            [korma.core :refer :all]
            ))

(def magnet-link "magnet:?xt=urn:btih:4015ab9713a0ff6e12167c5d71eba4c5975f8669&dn=Transcendence+%282014%29+WEB-DL+XviD-MAX&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80&tr=udp%3A%2F%2Ftracker.istole.it%3A6969&tr=udp%3A%2F%2Fopen.demonii.com%3A1337")

(def db nil)

(def albatross-db-file "albatross-test.db")

(def foobar-land  {:name "foobar land 2",
                   :state :created
                   :size 1234
                   :info-hash "foobarlandhash"
                   :files [{:path "foo" :size 32} {:path "bar" :size 16}]})

(defn init-db []
  (alter-var-root #'db (fn [db] (.start (albatross.db/create-database {:db-file albatross-db-file}))))
  (insert torrents (values foobar-land)))

(defn close-db []
  (utils/delete-file albatross-db-file)
  (alter-var-root #'db (fn [db] nil)))

(defn wrap-tests [fn]
  (init-db)
  (fn)
  (close-db))

(use-fixtures :each wrap-tests)

(deftest magnet-test
  (testing "should find the torrent and and name and set the original link"
    (let [m (parse-magnet magnet-link)]
      (is (= "4015ab9713a0ff6e12167c5d71eba4c5975f8669" (:hash m)))
      (is (= "Transcendence+(2014)+WEB-DL+XviD-MAX" (:name m) ))
      (is (= '("udp://tracker.openbittorrent.com:80&" "udp://tracker.publicbt.com:80&" "udp://tracker.istole.it:6969&" "udp://open.demonii.com:1337") (:trackers m))))))


(def tor-bytes (utils/to-byte-array "test/albatross/test/ubuntu-14.04.1-desktop-amd64.iso.torrent"))

(defn cmp-selected [expected actual]
  (= expected (select-keys actual (keys expected))))

(def ubuntu-t
  {:name "ubuntu-14.04.1-desktop-amd64.iso",
                         :state :created
                         :size 1028653056
                         :info-hash "cb84ccc10f296df72d6c40ba7a07c178a4323a14"
                         :files nil})

(deftest torrent-creation-test
  (testing "find-or-create-bytes"

    (let [t (find-or-create-by-bytes tor-bytes)]
      (is (cmp-selected ubuntu-t t))
      (is (= 2 (count (select torrents))))))

  (testing "find-shouldn't make a second if it exists"
    (let [t (find-or-create-by-bytes tor-bytes)
          t2 (find-or-create-by-bytes tor-bytes)]
      (is (= (:id t) (:id t2))))))

(deftest find-by-hash-test
  (testing "simple find by hash"
    (is (cmp-selected foobar-land (find-by-hash (:info-hash foobar-land)))))
  (testing "hash doesn't exist"
    (is (nil? (find-by-hash "3ksdfjsdlfjksdfjksdlfjkdslfjklsd")))))

(deftest update-torrent-test
  (testing "update-torrent"
    (update-torrent! (assoc (find-by-hash (:info-hash foobar-land)) :name "zombieland"))
    (is (= "zombieland" (:name (find-by-hash (:info-hash foobar-land)))))))

(deftest clear-db-test
  (testing "clearing db"
    (is (= 1 (count (select torrents))))
    (clear-db!)
    (is (= 0 (count (select torrents))))))

(deftest remove-torrent-test
  (testing "remove a single torrent"
    (remove-torrent! (find-by-hash (:info-hash foobar-land)))
    (is (= 0 (count (select torrents))))))

(deftest json-serialization-test
  (testing "sanity check"
    (is (= (:files foobar-land) (:files (find-by-hash (:info-hash foobar-land)))))))
