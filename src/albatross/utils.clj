(ns albatross.utils
  (:require [clojure.java.io :as io]))

(defn delete-file [f & [silently]]
  (or (.delete (io/file f))
      silently
      (throw (java.io.IOException. (str "Couldn't delete file " f)))))

(defn delete-file-recursively [f & [silently]]
  (let [f (io/file f)]
    (when (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (delete-file f silently)))

(defn to-byte-array [file]
  (with-open [output (java.io.ByteArrayOutputStream.)]
    (with-open [input (io/input-stream file)]
      (io/copy input output))
    (.toByteArray output)))
