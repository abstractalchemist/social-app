(ns social-app.db-lite-test
  (:require [clojure.test :refer :all]
            [social-app.db-lite :refer :all]))

(deftest test-app
  (testing "insert new user"
    (let [generated_key (add-user {:name "Jason Hirata" :email "foo@bar.com" :password "something" :salt "else" :algorithm "NOTGOOD"})]
      (is (not (nil? generated_key)))))
  (testing "get users"
    (let [generated_key (add-user {:name "Jason Hirata" :email "foo3@bar.com" :password "something" :salt "else" :algorithm "NOTGOOD"})
          user (internal-get {:id generated_key})]
      (is (not (nil? user))))))
