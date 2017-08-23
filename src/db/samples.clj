(ns db.samples
  (:require [social-app.utils :refer :all]
            [clojure.java.io :as io]))

(defn -main[& args]
  (with-open [r (io/writer "dumps/tables_samples.sql")]
    (let [algo "MD5"
          tags ["computer_science" "weiss" "pc_gaming" "admin" "sys_admin" "magical_girl" "friends" "fast" "super_saiyan" "eating" "training" "mu_s" "school_idol" "student_council" "fortune_telling"]
          samples [{:name "Jason Hirata" :email "foo@bar.com" :password "1qaz2wsx" :tags ["computer_science" "weiss" "pc_gaming"]}
                   {:name "Administrator" :email "admin@notsosecure.com" :password "@dM1nI5Tr@t0R" :tags ["admin" "sys_admin"]}
                   {:name "Nanoha Takamachi" :email "nanoha@nanoha-anime.com" :password "3edc4rfv" :tags ["magical_girl" "friends"]}
                   {:name "Fate Testarossa" :eamil "fate@nanoha-anime.com" :password "5tgb6yhn" :tags ["magical_girl" "fast"]}
                   {:name "Son Goku" :email "son@db-anime.com" :password "1qaz3edc" :tags ["super_saiyan" "eating"]}
                    {:name "Vegeta" :email "vegeta@db-anime.com" :password "2wsx4rfv" :tags ["super_saiyan" "training"]}
                    {:name "Eli Ayase" :email "elii@love-live-anime.com" :password "5tgb7ujm" :tags ["mu_s" "school_idol" "student_council"]}
                    {:name "Nozomi Tojo" :email "nozomi@love-live-anime.com" :password "5tgb7ujm" :tags ["mu_s" "school_idol" "fortune_telling"]}]]
      (doseq [t tags]
        (. r write (str "INSERT INTO test.t_tag (tag) VALUES (\"" t "\");"))
        (. r newLine))
    
      (doseq [[id {:keys [name email password] user_tags :tags}]
              (map list
                   (range 1 (inc (count samples)))
                   samples)]
                   
        (let [salt (generate-salt)
              passwd (hash-new-password password salt algo)]
          (. r write (str "INSERT INTO test.t_user (name,email,salt,password,algorithm) VALUES (\"" name "\",\"" email "\",\"" salt "\",\"" passwd "\",\"" algo "\");"))
          (. r newLine)
          (doseq [t user_tags]
            (. r write (str "INSERT INTO test.t_tag_user (tag_id, user_id) VALUES (" (inc (. tags indexOf t)) "," id ");"))
            (. r newLine))
            
          (. r newLine))))))
