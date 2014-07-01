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
    (is (=  (list (vec (vals test-db))) (by-state :created)))
;    (is (= '() (by-state :not-there)))
    ))
