(ns social-app.handler-test
  (:import [java.net URLEncoder])
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clj-json.core :as json]
            [clojure.java.io :as io]
            [social-app.handler :refer :all]))

(clear-table)

(defn get-cookie-map[headers]
  (let [{cookies "Set-Cookie"} headers
        [cookie] cookies]
    (let [[value path] (clojure.string/split cookie #";")]
      (let [[k v] (clojure.string/split value #"=")]
        {k v}))))

(defn map->form-encoded
  [input]
  (clojure.string/join "&" (map (fn[[k v]]  (str (clojure.string/replace (str k) ":" "") "=" (URLEncoder/encode v))) input)))

(defn login[email password]
  (app (-> (mock/request :post "/login")
           (mock/content-type "application/x-www-form-urlencoded")
           (mock/body (map->form-encoded {:email email :password password})))))
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


  (testing "POST /account"
    (let [{:keys [status]} (app (-> (mock/request :post "/account" )
                                    (mock/content-type "application/x-www-form-urlencoded")
                                    (mock/body (map->form-encoded {:name "Jason Hirata" :email "foo2@bar.com" :passwd "bad_password" :passwd-verify "bad_password"}))))]
      (is (= status 200))))
  
  (testing "POST /login"
    (let [{:keys [status headers]} (login "foo2@bar.com" "bad_password")] 
      (is (= status 200))))
  
  (testing "GET /login")

  (testing "GET /profile"
    (let [{:keys [headers status body]} (login "foo2@bar.com" "bad_password")]
      (if (= status 200)
        (let [cookie-map (get-cookie-map headers)]
          (let [{:keys [status body]} (app (-> (mock/request :get "/profile")
                                               (mock/header "Cookie" (str "ring-session=" (get cookie-map "ring-session")))))]
            (is (= status 200))
            (let [response (json/parse-string body)]
              (is (= (get response "email") "foo2@bar.com"))))))))

  (testing "GET /wall")
  
  (testing "GET /:id/wall")
  
  (testing "POST /:id/wall")
  
  (testing "GET /:id/profile"
    (let [{:keys [headers status body]} (login "foo2@bar.com" "bad_password")]
      (when (= status 200)
        (let [cookie-map (get-cookie-map headers)
              response body]

          (let [{:keys [status body]} (app (-> (mock/request :get (str "/" (:id response)  "/profile"))
                                               (mock/header "Cookie" (str "ring-session=" (get cookie-map "ring-session")))))]
            (is (= status 200))
            (when (= status 200)
              (let [response body]
                (= (:name response) "Jason Hirata")
                (= (:email response) "foo2@bar.com"))))))))

  (testing "POST /:id/profile")
  

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
