(ns albatross.test.tvdb
  (:require [clojure.test :refer :all]
            [albatross.tvdb :refer :all]
            [clojure.java.io :as io]
            [clojure.xml :as xml]
            ))


(defn load-xml [name]
  (io/input-stream (str "test/albatross/test/" name)))

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

(deftest xml-series-loading
  (testing "simple xml parsing"
    (is (= simple-result (parse-xml-response (load-xml "series-simple.xml")) ))
    )
  )
