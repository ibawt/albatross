(ns albatross.test.tvdb
  (:require [clojure.test :refer :all]
            [albatross.tvdb :refer :all]
            [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clj-http.fake :refer :all]
            ))


(defn load-test-xml [name]
  (slurp (str "test/albatross/test/" name)))

(deftest test-de-underscoreize
  (testing "deunderscoreize"
    (is (= :foo-bar (deunderscorize :foo_bar)))
    (is (= {:foo-bar "blah" :bar-foo "foo bar"}
           (deunderscore-keys {:foo_bar "blah" :bar_foo "foo bar"}))))

  (testing "underscoreize"
    (is (= :foo_bar (underscoreize :foo-bar)))
    (is (= {:foo_bar "blah" :bar_foo "foo bar"}
           (underscoreize-keys {:foo-bar "blah" :bar-foo "foo bar"}))))
  (testing "underscore to deunderscore to underscore"
    (let [v {:foo-bar "foo bar" :bar-foo "bar foo"}]
      (is (= v (deunderscore-keys (underscoreize-keys v)))))))

(deftest test-un-join-aliases
  (testing "join aliases"
    (is (= {:aliases "FOO|BAR|BAZ"}
           (join-aliases {:aliases ["FOO" "BAR" "BAZ"]})))
    (is (= {:blah "baz"}
           (join-aliases {:blah "baz"}))))

  (testing "unjoin aliases"
    (is (= {:aliases ["FOO" "BAR" "BAZ"]}
           (unjoin-aliases {:aliases "FOO|BAR|BAZ"})))
    (is (= {:blah "baz"}
           (unjoin-aliases {:blah "baz"})))))

(def simple-result
  [{:tvdb-id 77526
    :language "en"
    :name "Star Trek"
    :aliases ["Star Trek: The Original Series" "Star Trek: TOS"]
    :banner "graphical/77526-g7.jpg"
    :overview "An Overview"
    :first-aired "1966-09-08"
    :network "NBC"
    :imdb-id "tt0060028"
    :zap2it-id "SH003985"}
   {:tvdb-id 126391
    :language "en"
    :name "Star Trek: Odyssey"
    :banner "text/126391.jpg"
    :overview "Another overview"
    :first-aired "2007-09-01" }])

(def all-series-fixture
  {:tvdb-id 85019
   :name "Chopped"
   :banner "graphical/85019-g2.jpg"
   :overview "An Overview"
   :language "en"
   :network "Food Network"
   :imdb-id "tt1353281"
   :zap2it-id "EP01111785"
   :first-aired "2009-01-13"})


(def all-episodes-fixture
  [{:tvdb-id 4349271
     :name "Food Network Stars!"
     :number 1
     :first-aired "2012-07-22"
     :overview "An Overview"
     :season 0
     :production-code "CQSP06"
     :rating-count 0
     :series-id 85019
     :season-id 40534
     :filename "episodes/85019/4349271.jpg"
     :thumb-width 225
     :thumb-height 400
    }
   {:tvdb-id 4349267
    :name "Grill Masters: Part One"
    :number 2
    :first-aired "2012-07-22"
    :overview "An Overview"
    :season 0
    :production-code "CQSP01"
    :rating-count 0
    :series-id 85019
    :season-id 40534
    :filename "episodes/85109/4349267.jpg"
    :thumb-width 400
    :thumb-height 225
    }]
  )
(deftest search-series-test
  (with-fake-routes
    {#"(.*)"
     (fn [req] {:status 200, :headers {}, :body (load-test-xml "series-simple.xml") })
     }
    (testing "simple search"
      (is (= simple-result (search-series "Star Trek"))))))
