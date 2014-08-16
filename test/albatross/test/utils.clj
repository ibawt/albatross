(ns albatross.test.utils
  (:require [clojure.test :refer :all]
            [albatross.utils :refer :all]
            [clojure.java.io :as io]))

(def tmp-dir (io/file (System/getProperty "java.io.tmpdir") "test"))

(def test-files ["foobar", "barfoo",
                 {"baz" ["test" "test2"]
                  "blah" ["foo-two" "foo-three"
                          { "farther" ["file1" "file2"]}]}])

(defn- touch-file [f cur-dir]
  (with-open [stream (io/output-stream (apply io/file (conj cur-dir f)))]
    (.write stream (byte-array (map byte [0 1 2])))))

(defn- mkdir [f cur-dir]
  (.mkdir (apply io/file (conj cur-dir f))))

(defn- make-test-files [files & [cur-dir]]
  (doseq [f files]
    (if (string? f)
      (touch-file f cur-dir)
      (doseq [[k v] f]
        (mkdir k cur-dir)
        (make-test-files v (conj (vec cur-dir) k))))))

(deftest delete-recursive-dir-test
  (testing "Delete a directory tree"
    (.mkdir tmp-dir)
    (make-test-files test-files [(.getAbsolutePath tmp-dir)])
    (delete-file-recursively tmp-dir)
    (is (not (.exists tmp-dir))))
  (testing "silently flag for failure exceptions"
    (is (thrown? java.io.IOException (delete-file-recursively "file that isn't there" false)))))
