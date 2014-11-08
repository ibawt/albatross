(ns albatross.poller-test
  (:use clojure.test
        albatross.poller)
  (:require [com.stuartsierra.component :as component]
            [albatross.db :as db]))


;; (deftest poller-start-and-stop
;;   (testing "initial state"
;;     (let [poller (create-poller)]
;;       (is (nil? (:poller poller)))))
;;   (testing "starting and stopping poller"
;;     (with-redefs [check-seedbox (fn [t])]
;;       (let [poller (component/start (create-poller))]
;;         (is (some? (:poller poller)))
;;         (component/stop poller)))))
