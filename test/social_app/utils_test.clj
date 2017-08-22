(ns social-app.utils-test
  (:require [clojure.test :refer :all]
            [social-app.utils :refer :all]))

(deftest test-utils
    (testing "hashing"
    (let [salt (generate-salt)
          salt1 (string->byte-array salt)
          salt2 (byte-array->string salt1)]
      (is (= salt salt2))))

  (testing "password hashing"
    (let [password "P@SSW0RD%^"
          salt (generate-salt)
          hashed (hash-new-password password salt)]
      (is (seq hashed)))))
  
