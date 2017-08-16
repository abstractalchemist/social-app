(ns social-app.handler
  (:import [java.security MessageDigest SecureRandom])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [korma.core :as db]
            [korma.db :as other]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(def HASH_ALGO "SHA-256")

(other/defdb test
  (other/mysql {:user "root" :password "root" :port "32768" :db "test"}))

(db/defentity user
  (db/table "t_user"))


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
      (let [ba (byte-array 512)]
        (. sc nextBytes ba)
        (byte-array->string ba)))))


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
  (let [[salt] (db/select user
                         (db/fields :salt)
                         (db/where {:email email}))]
    (hash-new-password password salt)))

    
(defroutes app-routes
  (GET "/" [] "Hello World")

  ;; login url
  (POST "/login" [email password]
        (let [hashed-input (hash-password email password)
              [passwd] (db/select user
                                  (db/fields :password)
                                  (db/where {:email email}))]
          (if (= hashed-input passwd)
            {:status 200
             :session {:recreate true
                       :logged-in true}}
            {:status 500})))

  ;; create a new account
  (POST "/account" [name email passwd passwd-verify]
        (println "email " email)
        (let [salt (generate-salt)
              hash1 (hash-new-password passwd salt)
              hash2 (hash-new-password passwd-verify salt)]
          (if (= hash1 hash2)
            (do
              (db/insert user
                         (db/values {:name name
                                     :email email
                                     :password hash1}))
              {:status 200})
            {:status 500})))
                      

  ;; get information assuming login
  (context "/:id" [:as {:keys [session]}]
           (GET "/profile" [])
           (POST "/profile" [])
           (GET "/wall" [])
           (POST "/wall" []))

  ;; other stuff
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes (assoc site-defaults :security {:anti-forgery false})))
