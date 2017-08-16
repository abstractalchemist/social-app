(ns social-app.handler
  (:import [java.security MessageDigest SecureRandom]
           [java.text SimpleDateFormat]
           [java.util Calendar])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [clj-json.core :as json]
            [korma.core :as db]
            [korma.db :as other]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(def HASH_ALGO "SHA-256")

(other/defdb test
  (other/mysql {:user "root" :password "root" :port (or (System/getProperty "db.port") "32768") :db "test" :host (or (System/getProperty "db.host") "localhost")}))

(db/defentity user
  (db/table "t_user"))

(db/defentity wall
  (db/table "t_wall"))

(defn clear-table[]
  (db/delete user)
  (db/delete wall))

(defn string->byte-array
  "Convert salt string into byte array"
  [salt]
  (into-array Byte/TYPE (map #(Byte/parseByte (str %) 16) salt)))

(defn byte-array->string
  "convert a byte array into a string"
  [byte-ar]
  (clojure.string/join (map #(String/format "%X" (into-array [%])) byte-ar )))

(def generate-salt
  "generate salt string"
  (let [sc (new SecureRandom)]
    (fn[]
      (let [ba (byte-array 256)]
        (. sc nextBytes ba)
        (byte-array->string ba)))))

(defn internal-get[conditions]
  (let [[profile] (db/select user
                             (db/fields :id
                                        :name
                                        :email
                                        :salt
                                        :password)
                             (db/where conditions))]
    profile))

(def get-profile
  ""
  (fn ([id] (get-profile id false))
    ([id public?]
     (let [[profile]
           (if public?
             (db/select user
                        (db/fields :name
                                   :email)
                        (db/where {:id id}))
             (db/select user
                        (db/fields :name)
                        (db/where {:id id})))]
       (println profile)
       profile))))

(def TIME_FORMATTER
  (new SimpleDateFormat "yyyy-MM-dd HH:mm:ss"))


(defn hash-new-password
  ""
  [password salt]
  {:pre [(seq password) (seq salt)]}

           
  (let [md (MessageDigest/getInstance HASH_ALGO)
        passwd-byte (. password getBytes)
        salt-byte (string->byte-array salt)]
    (. md update salt-byte)
    (. md update passwd-byte)
    (byte-array->string (. md digest))))

(defn hash-password[email password]
  (let [[{:keys [salt]}] (db/select user
                                    (db/fields :salt)
                                    (db/where {:email email}))]
    (hash-new-password password salt)))

(defn get-wall
  "get a user wall"
  [id]
  (db/select wall
             (db/fields :comment
                        :id
                        :at)
             (db/where {:user_id id})))
    
(defroutes app-routes
  (GET "/" [] "Hello World")

  ;; login url
  (POST "/login" [email password]
        (let [hashed-input (hash-password email password)
              profile (internal-get {:email email})
              {:keys [password id]} profile]
          (if (= hashed-input password)
            {:status 200
             :session {:recreate true
                       :id id
                       :logged-in true}
             :body profile}
            {:status 500})))

  ;; create a new account
  (POST "/account" [name email passwd passwd-verify]
        (if (and (seq name) (seq email) (seq passwd) (seq passwd-verify))
          (let [salt (generate-salt)
                hash1 (hash-new-password passwd salt)
                hash2 (hash-new-password passwd-verify salt)]
            (if (= hash1 hash2)
            (do
              (db/insert user
                         (db/values {:name name
                                     :email email
                                     :salt salt
                                     :password hash1}))
              {:status 200})
            {:status 500
             :body "password information invalid"}))
          {:status 500
           :body "Invalid Information"}))
                      

  ;; get information assuming login
  (context "/:id" [id :as {{session-id :id logged-in :logged-in} :session}]
           (GET "/profile" []
                (if-let [profile (get-profile id (= session-id id))]
                  
                  {:status 200
                   :headers {"content-type" "application/json"}
                   :body profile }
                  {:status 404}))
           (POST "/profile" [])
           (GET "/wall" []
                (if logged-in
                  {:status 200
                   :headers {"content-type" "application/json"}
                   :body (get-wall id)}))
           (DELETE "/wall/:id" []
                   (if logged-in
                     (do
                       (db/delete wall (db/where {:id id}))
                       {:status 200})))
           (POST "/wall" [:as body]
                 (if (= session-id id)
                   (let [input (json/parsed-seq (io/reader body))]
                     (db/insert wall
                                (db/values {:comment (get input "comment")
                                            :at (. TIME_FORMATTER (Calendar/getInstance))}))
                     {:status 200})
                   {:status 500})))
                                            

  ;; other stuff
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes (assoc site-defaults :security {:anti-forgery false})))
