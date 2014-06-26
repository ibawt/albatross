(ns albatross.test.provider
  (:use clojure.test
        ring.mock.request
        albatross.provider))


(def test-providers
  { "some name" {:foo "bar"
                 :bar "foo" }
    "some other name" {:foo "bar"
                       :bar "foo"
                       :baz "baz"
                       }})

(reset! providers test-providers)

(deftest filter-config-test
  (testing "filter should return a list properties of provider that isn't nil"
    (is (= (filter-config :foo)
           '("bar" "bar")))
    (is (= (filter-config :baz)
           '("baz")))))
