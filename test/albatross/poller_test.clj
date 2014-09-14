(ns albatross.poller-test
  (:use clojure.test
        albatross.poller)
  (:require [com.stuartsierra.component :as component]
            [albatross.db :as db]))


(deftest poller-start-and-stop
  (testing "initial state"
    (let [poller (create-poller)]
      (is (false? @(:running poller)))
      (is (nil? (:poller poller)))))
  (testing "starting poller"
    (let [poller (component/start (create-poller))]
      (is (future? (:poller poller)))
      (is @(:running poller))
      (component/stop poller))))
