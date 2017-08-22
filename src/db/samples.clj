(ns db.samples
  (:require [social-app.utils :refer :all]
            [clojure.java.io :as io]))

(defn -main[& args]
  (with-open [r (io/writer "dumps/tables_samples.sql")]
    (let [algo "MD5"]
      (doseq [{:keys [name email password]}  [{:name "Jason Hirata" :email "foo@bar.com" :password "1qaz2wsx"}
                                              {:name "Nanoha Takamachi" :email "nanoha@nanoha-anime.com" :password "3edc4rfv"}
                                              {:name "Fate Testarossa" :eamil "fate@nanoha-anime.com" :password "5tgb6yhn"}
                                              {:name "Son Goku" :email "son@db-anime.com" :password "1qaz3edc"}
                                              {:name "Vegeta" :email "vegeta@db-anime.com" :password "2wsx4rfv"}
                                              {:name "Nozomi Tojo" :email "nozomi@love-live-anime.com" :password "5tgb7ujm"}]]
        (let [salt (generate-salt)
              passwd (hash-new-password password salt algo)]
          (. r write (str "INSERT INTO test.t_user (name,email,salt,password,algorithm) VALUES (\"" name "\",\"" email "\",\"" salt "\",\"" passwd "\",\"" algo "\");"))
          (. r newLine))))))
