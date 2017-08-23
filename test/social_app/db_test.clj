(ns social-app.db-test
  (:require [clojure.test :refer :all]
            [social-app.db :refer :all]))

(deftest test-app
  (testing "search"
    (let [results (search 0 "magical_girl")]
      (is (= 2 (count results))))))
