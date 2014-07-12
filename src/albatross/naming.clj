(ns albatross.naming
  (:require [albatross.tvdb :as tvdb]
            [taoensso.timbre :as timbre]
            [clojure.string :as str]))

(def numerals {\I 1, \V 5, \X 10, \L 50, \C 100, \D 500, \M 1000})

(defn- add-numeral [n t]
  (if (> n (* 4 t))
    (- n t)
    (+ t n)))

(defn roman [s]
  (reduce add-numeral (map numerals (reverse s))))

(timbre/refer-timbre)


(def clean-subs
  '((#"(\D)\.(?!\s)(\D)" "$1 $2")
    (#"(\d)\.(\d{4})" "$1 $2")
    (#"(\D)\.(?!\s)" "$1 ")
    (#"\.(?!\s)(\D)" "$1")
    (#"_" " ")
    (#"-$" "")))



(defn clean-name [name]
  (reduce (fn [n [regex replace]]
            (.replaceAll (re-matcher regex n) replace)) name clean-subs))


(defn analyze [name]
  )
