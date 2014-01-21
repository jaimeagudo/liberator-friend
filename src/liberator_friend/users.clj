(ns liberator-friend.users
  (:require [cemerick.friend.credentials :refer (hash-bcrypt)]))

; "The Database"
; "dummy in-memory user database."
(def users ;(atom
             {
	"root" {
		:username "root"
		:password (hash-bcrypt "password")
		; :pin "1234" ;; only used by multi-factor
		:roles #{:admin}}
    "jaime" {
    	:username "jaime"
    	:password (hash-bcrypt "password")
    	:roles #{:user}}
})
; )

; (derive :admin :user)


(defn create-user
  [{:keys [username password admin] :as user-data}]
  (-> (dissoc user-data :admin)
      (assoc :identity username
             :password (hash-bcrypt password)
             :roles (into users (when admin [:admin])))))