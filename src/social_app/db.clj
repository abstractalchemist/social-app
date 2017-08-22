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
                   (or info (let [{:keys [generated_key]} (db/insert tag (db/values {:tag _tag}))]
                              generated_key)))]
    (db/insert tag_user (db/values {:tag_id tag_info :user_id id}))))

        
      

(defn delete-tag
  "delete a tag from a user profile"
  [id _tag]
  (when-let [[{tag_id :id}] (db/select tag (db/fields :id) (db/where {:tag _tag}))]
    
    (db/delete tag_user (db/where {:tag_id tag_id :user_id id}))))
