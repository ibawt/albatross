(ns albatross.providers.utils)

(defn format-number [s]
  (format "%02d" (Integer/parseInt s)))

(defn make-search-query [params]
  (str
   (:show_name params)
   " S"
   (format-number (:season params))
   "E"
   (format-number (:episode params))))
