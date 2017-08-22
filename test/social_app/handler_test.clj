(ns social-app.handler-test
  (:import [java.net URLEncoder])
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clj-json.core :as json]
            [clojure.java.io :as io]
            [social-app.db :refer [clear-table]]
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
  
  (testing "GET /profile"
    (let [{:keys [status]} (app (mock/request :get "/profile"))]
      (is (= status 500)))

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

  (testing "GET /profile/tags"
    (let [{:keys [status]} (app (mock/request :get "/profile/tags"))]
          (is (= status 500)))
    (let [{:keys [headers status body]} (login "foo2@bar.com" "bad_password")]
      (when (= status 200)
        (let [cookie-map (get-cookie-map headers)
              response body]
          (let [{:keys [status body]} (app (-> (mock/request :get "/profile/tags")
                                               (mock/header "Cookie" (str "ring-session=" (get cookie-map "ring-session")))))]
            (is (= status 200)))))))
  
  (testing "POST /profile/tags/:tag"
    (let [{:keys [status]} (app (mock/request :post "/profile/tags/saiyan"))]
      (is (= status 500)))
        
    (let [{:keys [headers status body]} (login "foo2@bar.com" "bad_password")]
      (when (= status 200)
        (let [cookie-map (get-cookie-map headers)
              response body]
          (let [{:keys [status body]} (app (-> (mock/request :post "/profile/tags/computer_science")
                                               (mock/header "Cookie" (str "ring-session=" (get cookie-map "ring-session")))))]
            (is (= status 200))
            (let [{:keys [status body]} (app (-> (mock/request :get "/profile/tags")
                                                 (mock/header "Cookie" (str "ring-session=" (get cookie-map "ring-session")))))]
              (is (= status 200))
              (let [[response] (json/parse-string body)]
                (println "response " response)
                         
                (is (= "computer_science" (get response "tag"))))))))))

  (testing "DELETE /profile/tags/:tag"
    (let [{:keys [status]} (app (mock/request :delete "/profile/tags/weiss"))]
      (is (= status 500)))
        
    (let [{:keys [headers status body]} (login "foo2@bar.com" "bad_password")]
      (when (= status 200)
        (let [cookie-map (get-cookie-map headers)
              response body]
          (let [{:keys [status body]} (app (-> (mock/request :delete "/profile/tags/computer_science")
                                               (mock/header "Cookie" (str "ring-session=" (get cookie-map "ring-session")))))]
            (is (= status 200))
            (let [{:keys [status body]} (app (-> (mock/request :get "/profile/tags")
                                            (mock/header "Cookie" (str "ring-session=" (get cookie-map "ring-session")))))]
              (= status 200)
              (println body)
              (is (= 0 (count (json/parse-string body))))))))))

  

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
