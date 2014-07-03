(ns albatross.test.torrentdb
  (:use clojure.test
        ring.mock.request
        albatross.torrentdb))

(def test-db
  {"993292992dfasd9032"
   {:hash "993292992dfasd9032"
    :name "Some torrent"
    :state :created
    :files [{"length" 65, "path" ["Some file.txt"]}
            {"length" 3434343, "path" ["downloads" "filename.avi"]}]}})

(reset! db test-db)

(deftest filtering-by-state
  (testing "by-state"
    (is (= (list (first test-db)) (by-state :created)))
    (is (= '() (by-state :not-there)))))

(def magnet-link "magnet:?xt=urn:btih:4015ab9713a0ff6e12167c5d71eba4c5975f8669&dn=Transcendence+%282014%29+WEB-DL+XviD-MAX&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80&tr=udp%3A%2F%2Ftracker.istole.it%3A6969&tr=udp%3A%2F%2Fopen.demonii.com%3A1337")

(deftest magnet-test
  (testing "should find the torrent and and name and set the original link"
    (let [m (parse-magnet magnet-link)]
      (is (= "4015ab9713a0ff6e12167c5d71eba4c5975f8669" (:hash m)))
      (is (= "Transcendence+%282014%29+WEB-DL+XviD-MAX&tr" (:name m)))
      (is (= '("udp://tracker.openbittorrent.com:80&" "udp://tracker.publicbt.com:80&" "udp://tracker.istole.it:6969&" "udp://open.demonii.com:1337") (:trackers m))))))
