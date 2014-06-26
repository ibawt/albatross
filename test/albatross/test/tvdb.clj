(ns albatross.test.tvdb
  (:require [clojure.test :refer :all]
            [albatross.tvdb :refer :all]))


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
           (unjoin-aliases {:blah "baz"})))
    )
  )
