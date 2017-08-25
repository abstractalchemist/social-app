(ns social-app.db-lite
  (:require [korma [core :as db]
             [db :as other]]))

(other/defdb test
  (other/mysql {:user "root" :password "root" :port (or (System/getProperty "db.port") "32768") :db "test" :host (or (System/getProperty "db.host") "localhost")}))

(defn convert-conditions[condition-map]
  (clojure.string/join " AND " (map (fn[[k v]] (str (clojure.string/replace (str k) #":" "") " = " (if (string? v) (str "'" v "'") v) ))  condition-map)))

(defn internal-get[conditions]
  (let [select (str "select * from t_user where " (convert-conditions conditions))]
    (first (db/exec-raw select  :results))))

(defn add-user[{:keys [name email password salt algorithm]}]
  (let [insert (str "insert into t_user (name,email,password,salt,algorithm) values ('" name "','" email "','" password "','" salt "','" algorithm  "')")]
    (:generated_key (db/exec-raw insert :keys))))

(defn get-user-tags[id]
  (let [select (str "select tag from t_tag where id in ( select tag_id from t_tag_user where user_id = " id ")")]
    (db/exec-raw select :results)))

(defn search[user-id & [terms]]
  (let [select (str "select id,name from t_user where id in (select user_id from t_tag join t_tag_user where t_tag.id = t_tag_user.tag_id and t_tag.tag like " terms ")")]
    (println " using " select)
    (db/exec-raw select :results)))

(defmacro get-all-users[conditions])

(defn get-profile
  ([id] (get-profile id false))
  ([id private?]
   (if private?
     (let [[private_profile] (db/exec-raw (str "select name,email from t_user where id= " id) :results)]
       (assoc private_profile :tags (get-user-tags id)))
     (db/exec-raw (str "select name from t_user where id= " id) :results))))
   
   

(defn add-tag[id tag]
  (let [[results] (db/exec-raw (str "select * from t_tag where tag = '" tag "'") :results)]
    (let [tag_id (if results
                   (:id results)
                   (:generated_key (db/exec-raw (str "insert into t_tag ( tag ) values ('" tag "')") :keys)))]
      (:generated_key (db/exec-raw (str "insert into t_tag_user ( tag_id, user_id ) VALUES ( " tag_id "," id " )"))))))

(defn delete-tag[id tag]
  (db/exec-raw (str "delete from t_tag_user where user_id = " id " AND tag_id = (select id from t_tag where tag = '" tag "')")))

(defn get-wall[])
(defn update-wall[])
(defn delete-wall-comment[])
