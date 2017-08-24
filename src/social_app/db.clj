(ns social-app.db
  (:require [korma
             [core :as db]
             [db :as other]]))
 
(declare get-user-tags)

(other/defdb test
  (other/mysql {:user "root" :password "root" :port (or (System/getProperty "db.port") "32768") :db "test" :host (or (System/getProperty "db.host") "localhost")}))

(db/defentity user
  (db/table "t_user"))

(db/defentity wall
  (db/table "t_wall"))

(db/defentity tag
  (db/table "t_tag"))

(db/defentity tag_user
  (db/table "t_tag_user"))

(defn clear-table[]
  (db/delete user)
  (db/delete wall)
  (db/delete tag_user)
  (db/delete tag))

(defn add-user[user-data]
  (:generated_key (db/insert user (db/values user-data))))

(defmacro get-all-users[conditions]
  `(db/select user (db/fields :name
                              :id)
              (db/where ~conditions)))
 
(defn internal-get[conditions]
  (first
   (db/select user
              (db/fields :id
                         :name
                         :email
                         :salt
                         :password
                         :algorithm)
              (db/where conditions))))


(def get-profile
  ""
  (fn ([id] (get-profile id false))
    ([id private?]
     (let [[profile]
           (if private?
             (db/select user
                        (db/fields :name
                                   :email)
                        (db/where {:id id}))
             (db/select user
                        (db/fields :name)
                        (db/where {:id id})))]
       (println profile)
       (if private?
         (assoc profile :tags (get-user-tags id))
         profile)))))

(defn update-wall[values]
  (:generated_key (db/insert wall (db/where values))))

(defn delete-wall-comment[id]
  (db/delete wall (db/where {:id id})))

(defn get-wall
  "get a user wall"
  [id]
  (db/select wall
             (db/fields :comment
                        :id
                        :at)
             (db/where {:user_id id})))

(defn get-user-tags
  "get all user tags"
  [id]
  (let [tag_list (db/select tag
                            (db/fields :tag)
                            (db/where {:id [in (db/subselect tag_user
                                                          (db/fields :tag_id)
                                                          (db/where {:user_id id}))]}))]
    tag_list))

(defn add-tag
  "add a tag to a user profile"
  [id _tag]
  ;; check if a tag exists, create it
  (let [tag_info (let [[info] (db/select tag (db/fields :id)
                                               (db/where {:tag _tag}))]
                         
                   (or (:id info) (let [{:keys [generated_key]} (db/insert tag (db/values {:tag _tag}))]
                                    (println "Using generated key: " generated_key)
                                    generated_key)))]
    (println "inserting map " tag_info " for user " id)
    (db/insert tag_user (db/values {:tag_id tag_info :user_id id}))))

        
      

(defn delete-tag
  "delete a tag from a user profile"
  [id _tag]
  (when-let [[{tag_id :id}] (db/select tag (db/fields :id) (db/where {:tag _tag}))]
    
    (db/delete tag_user (db/where {:tag_id tag_id :user_id id}))))


(defn search
  ""
  [user-id & terms]
  (let [term-map (db/subselect tag_user
                            (db/fields :user_id)
                            (db/where (and (not (= :user_id user-id))
                                           {:tag_id [in (db/subselect tag
                                                                      (db/fields :id)
                                                                      (db/where {:tag [like terms]}))]})))]
    (when (< 0 (count term-map))
      (db/select user
                 (db/fields :id
                            :name)
                 (db/where {:id [in term-map]})))))
                                                                 
