(ns albatross.seedbox-test
  (:use clojure.test
        albatross.seedbox
        ring.mock.request)
  (:require [com.stuartsierra.component :as component]
            [necessary-evil.core :as xmlrpc]))

(def config {:rtorrent {:username "foo"
                        :password "bar"
                        :endpoint-url "http://foobar.com/RPC"}})

(defn with-call-mock [f response]
  (with-redefs-fn {#'xmlrpc/call* (fn [endpoint cmd data & params]
                                    response)} f))

(deftest seedbox-init-test
  (testing "should read the correct config values"
    (let [s (create-seedbox config)]
      (is (= ["foo" "bar"] (:credentials s)))
      (is (= "http://foobar.com/RPC" (:endpoint-url s)))))
  (testing "start and stop"
    (let [s (create-seedbox config)]
      (is (= s (component/start s)))
      (is (= s (component/stop s)))))
  (testing "multiple starts"
    (let [s (create-seedbox config)]
      (let [s2 (component/start s)]
        (let [s3 (component/start s)]
          (is (= s2 s3)))))))

(deftest cmd-test
  (let [s (create-seedbox config)]
    (testing "should take the action and add d. to the front"
      (with-redefs [xmlrpc/call* (fn [e c d & params]
                                     (is (= :d.foo c)))]
        (cmd s :foo "somehash")))

    (testing "cmd-p will implicity do an == 1"
      (with-redefs
        [xmlrpc/call* (fn [e c d & params] "1")]
        (is (= true (cmd-p s :foo "somehash"))))
      (with-redefs
        [xmlrpc/call* (fn [e c d & params] "0")]
        (is (= false (cmd-p s :foo "somehash")))))))

(def torrent-list-response
  [[:name "NAME"
    :hash "hash"
    :connection_current "connection_current"
    :size_bytes "size_bytes"
    :completed_bytes "completed_bytes"
    :creation_date "creation_date"
    :bytes_done "bytes_done"
    :up.rate "up.rate"
    :down.rate "down.rate"
    :get_message "get_message"
    :peers_complete "peers_complete"
    :peers_connected "peers_connected"
    :state "state"
    :complete "complete"
    :is_active "is_active"
    :is_hash_checked "is_hash_checked"
    :is_multi_file "is_multi_file"
    :is_open "is_open"
    :is_private "is_private"]

   [:name "FOOBA333R"
    :hash "323234233"
    :connection_current "sdfdsfofodso"
    :size_bytes 3323
    :completed_bytes 3392
    :creation_date "creation-date"
    :bytes_done 9982
    :up.rate "1"
    :down.rate 32
    :get_message "ds"
    :peers_complete "3"
    :peers_connected "12"
    :state "3"
    :complete "1"
    :is_active "1"
    :is_hash_checked "0"
    :is_multi_file "0"
    :is_open "1"
    :is_private "0"]])

(def to-response
  (map (fn [x] (keep-indexed #(if (odd? %1) %2) x)) torrent-list-response))

(deftest list-torrents-test
  (let [s (create-seedbox config)]
    (testing "get the list of torrents into a list of maps"
      (with-redefs [xmlrpc/call* (fn [e c d & r]
                                   (vec to-response))]
        (is (= (map (partial apply hash-map) torrent-list-response)
               (list-torrents s)))))))
