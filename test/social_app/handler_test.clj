(ns social-app.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [social-app.handler :refer :all]))

(deftest test-app
  (testing "hashing"
    (let [salt (generate-salt)
          salt1 (string->byte-array salt)
          salt2 (byte-array->string salt1)]
      (is (= salt salt2))))

  (testing "password hashing"
    (let [password "P@SSW0RD%^"
          salt (generate-salt)
          hashed (hash-new-password password salt)]
      (is (seq hashed))))
      
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "POST /login")

  (testing "GET /login")

  (testing "GET /:id/wall")

  (testing "POST /:id/wall")

  (testing "GET /:id/profile")

  (testing "POST /:id/profile")

  (testing "POST /account"
    (let [{:keys [status]} (app (-> (mock/request :post "/account" )
                                    (mock/content-type "application/x-www-form-urlencoded")
                                    (mock/body "email=foo@bar.com&passwd=bad_password&passwd-verify=bad_password")))]
      (is (= status 200))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
