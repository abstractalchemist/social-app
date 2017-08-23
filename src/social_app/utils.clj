(ns social-app.utils
  (:import [java.security MessageDigest SecureRandom]))

(defonce HASH_ALGO "SHA-256")

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

(defn hash-new-password
  ""
  ([password salt] (hash-new-password password salt HASH_ALGO))
  ([password salt hash-algo]
   {:pre [(seq password) (seq salt)]}
   
   (println "***** using " hash-algo " *******")
   (let [md (MessageDigest/getInstance hash-algo)
         passwd-byte (. password getBytes)
         salt-byte (string->byte-array salt)]
     (. md update salt-byte)
     (. md update passwd-byte)
     (byte-array->string (. md digest)))))
