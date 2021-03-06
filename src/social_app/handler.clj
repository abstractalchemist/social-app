(ns social-app.handler
  (:import [java.text SimpleDateFormat]
           [java.util Calendar])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [clj-json.core :as json]
            [korma.core :as db]
            [social-app.utils :refer [HASH_ALGO string->byte-array byte-array->string hash-new-password generate-salt]]
            [social-app.db-lite :refer [internal-get add-user get-user-tags search get-all-users get-profile add-tag delete-tag get-wall update-wall delete-wall-comment]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(def TIME_FORMATTER
  (new SimpleDateFormat "yyyy-MM-dd HH:mm:ss"))

(defn hash-password
  ([email password] (hash-password email password (:algorithm (internal-get {:email email}))))
  ([email password hash-algo]
   (let [{:keys [salt]} (internal-get {:email email})]
     (hash-new-password password salt hash-algo))))
                                 

(defn sanitize
  "sanitize the search terms to prevent sql and xss issues"
  [terms]
  terms)

(defroutes app-routes
  (GET "/" [] "Hello World")

  ;; login url
  (POST "/login" [email password]
;;  	(println "Using " email " and password " password)
        (let [hashed-input (hash-password email password)
              profile (internal-get {:email email})
              {:keys [password id]} profile]
          (if (= hashed-input password)
            {:status 200
             :session {:recreate true
                       :id id
                       :logged-in true}
             :headers { "content-type" "application/json" }
             :body (json/generate-string profile)}
            {:status 500})))

  ;; create a new account
  (POST "/account" [name email passwd passwd-verify]
        (if (and (seq name) (seq email) (seq passwd) (seq passwd-verify))
          (let [salt (generate-salt)
                hash1 (hash-new-password passwd salt)
                hash2 (hash-new-password passwd-verify salt)]
            (if (= hash1 hash2)
              (let [generated_key (add-user {:name name
                                             :email email
                                             :salt salt
                                             :password hash1
                                             :algorithm HASH_ALGO })]
              {:status 200
               :headers {"content-type" "application/json"}
               :session {:recreate true
                         :id generated_key}})
            {:status 500
             :body "password information invalid"}))
          {:status 500
           :body "Invalid Information"}))

  (POST "/logout" [:as {{session-id :id} :session}]
        (if session-id
          {:status 200
           :session {:recreate true
                     :id nil}}
          {:status 200}))

  (GET "/profiles" [searchTerms :as {{session-id :id} :session}]
       (if session-id
         (let [sanitize-term (sanitize searchTerms)]
           (letfn [(user-map [{:keys [id name]}]
                     (println "mapping " name " to " id)
                     {:name name :tags (get-user-tags id) :id id})]
             (println "using session-id " session-id)
             (if (and (seq searchTerms))
               {:status 200
                :headers {"content-type" "application/json"}
                :body (json/generate-string (map
                                             user-map
                                             (search session-id sanitize-term)))}
                                                                       
               {:status 200
                :headers {"content-type" "application/json"}
                :body (json/generate-string (map
                                             user-map
                                             (db/exec-raw (str "select id,name from t_user where not t_user.id = " session-id) :results)))})))
                                             ;;(get-all-users (not (= :id session-id)))))})))
           
         {:status 500}))
       

  (GET "/profile" [:as {{session-id :id :as session-map} :session headers :headers}]
       (println "Retrieving profile for " session-id)
       (if session-id
         {:status 200
          :headers {"content-type" "application/json"}
          :body (json/generate-string (get-profile session-id true))}
         {:status 500}))

  (GET "/profile/tags" [:as {{session-id :id :as session-map} :session}]
       (if session-id
         {:status 200
          :headers {"content-type" "application/json"}
          :body (json/generate-string (get-user-tags session-id))}
         {:status 500}))
  (POST "/profile/tags/:tag" [tag :as {{session-id :id :as session-map} :session}]
        (if session-id
          (do
            (add-tag session-id tag)
            {:status 200})
          {:status 500}))
  (DELETE "/profile/tags/:tag" [tag :as {{session-id :id :as session-map} :session}]
          (if session-id
            (do
              (delete-tag session-id tag)
              {:status 200})
            {:status 500}))
  
  (POST "/wall" [:as {{session-id :id} :session}])
  (GET "/wall" [:as {{session-id :id} :session}])
  
  ;; get information assuming login
  (context "/:id" [id :as {{session-id :id logged-in :logged-in} :session}]
           (GET "/profile" []
                (if-let [profile (get-profile id (= session-id id))]
                  
                  {:status 200
                   :headers {"content-type" "application/json"}
                   :body (json/generate-string profile) }
                  {:status 404}))
           (GET "/tags" []
                (if-let [profile (get-profile id)]
                  {:status 200
                   :headers {"content-type" "application/json"}
                   :body (json/generate-string (get-user-tags id))}
                  {:status 404}))
           (POST "/profile" [])
           (GET "/wall" []
                (if logged-in
                  {:status 200
                   :headers {"content-type" "application/json"}
                   :body (json/generate-string (get-wall id)) }))
           (DELETE "/wall/:id" []
                   (if logged-in
                     (do
                       (delete-wall-comment {:id id})
                       {:status 200})))
           (POST "/wall" [:as body]
                 (if (= session-id id)
                   (let [input (json/parsed-seq (io/reader body))]
                     (update-wall {:comment (get input "comment")
                                   :at (. TIME_FORMATTER (Calendar/getInstance))})
                     {:status 200})
                   {:status 500})))
  

  ;; other stuff
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes (assoc site-defaults :security {:anti-forgery false})))
