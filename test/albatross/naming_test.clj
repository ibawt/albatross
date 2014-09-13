(ns albatross.naming-test
  (:use clojure.test
        albatross.naming))

(deftest roman-numeral-test
  (testing "1 to 10"
    (is (= 1 (roman "I")))
    (is (= 2 (roman "II")))
    (is (= 3 (roman "III")))
    (is (= 4 (roman "IV")))
    (is (= 5 (roman "V")))
    (is (= 6 (roman "VI")))
    (is (= 7 (roman "VII")))
    (is (= 8 (roman "VIII")))
    (is (= 9 (roman "IX")))
    (is (= 10 (roman "X"))))
  (testing "years"
    (is (= 2003 (roman "MMIII")))
    (is (= 2014 (roman "MMXIV")))
    (is (= 1990 (roman "MCMXC"))))
  (testing "some random other numbers"
    (is (= 48 (roman "XLVIII")))
    (is (= 76 (roman "LXXVI")))))

;; (deftest clean-name-test
;;   (let [result "an example 1.0 test"]
;;     (testing "some names"
;;       (is (= result (clean-name "an_example_1.0_test")))
;;       (is (= result (clean-name "an.example.1.0.test"))))))
