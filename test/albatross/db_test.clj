(ns albatross.db-test
  (:use clojure.test
        albatross.db)
  (:require [albatross.utils :as utils]))

(def config {:db-file "albtross-test.db"})

(defn- delete-db! []
  (utils/delete-file (:db-file config) true))

(defn- wrap-tests [fn]
  (fn)
  (delete-db!))

(use-fixtures :each wrap-tests)

(deftest test-de-underscoreize
  (testing "deunderscoreize"
    (is (= :foo-bar (deunderscorize :foo_bar)))
    (is (= {:foo-bar "blah" :bar-foo "foo bar"}
           (deunderscore-keys {:foo_bar "blah" :bar_foo "foo bar"}))))

  (testing "underscoreize"
    (is (= :foo_bar (underscoreize :foo-bar)))
    (is (= {:foo_bar "blah" :bar_foo "foo bar"}
           (underscore-keys {:foo-bar "blah" :bar-foo "foo bar"}))))
  (testing "underscore to deunderscore to underscore"
    (let [v {:foo-bar "foo bar" :bar-foo "bar foo"}]
      (is (= v (deunderscore-keys (underscore-keys v)))))))
