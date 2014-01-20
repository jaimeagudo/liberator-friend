(ns liberator-friend.core
  "Core namespace for the liberator-friend post."
  (:gen-class)
  (:use    [liberator-friend.util]
    [liberator-friend.users])
  (:require [cheshire.core :refer :all]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [compojure.handler :refer [api]]
            [compojure.core :as compojure :refer (GET ANY defroutes)]
            [liberator-friend.middleware.auth :as auth]
            [clojure.contrib.string :as str]
            [liberator-friend.resources :as r :refer [defresource]]
            [org.httpkit.server :refer [run-server]]
             [ring.middleware.reload :as rl]
            ))

;; ## Site Resources

(defresource admin-resource
  :base (r/role-auth #{:admin})
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  :handle-ok (fn [ctxt] (generate-string "Welcome, admin!")))

(defresource user-resource
  :base (r/role-auth #{:user})
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  :handle-ok (fn [ctxt] (generate-string "Welcome, user!")))

(defresource authenticated-resource
  :base r/authenticated-base
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  :handle-ok (fn [ctxt] (generate-string "Come on in. You're authenticated.")))

;; ## Compojure Routes

(defroutes site-routes
  (GET "/" [] "Welcome to the liberator-friend demo site!")
  (GET "/admin" [] admin-resource)
  (GET "/authenticated" [] authenticated-resource)
  (GET "/user" [] user-resource)


  (GET "/api/ping" ping)
  (ANY "/session" session-resource)
  ; (GET "/api/session" json-auth/handle-session)
  ; (POST "/api/session" json-auth/handle-session)
  ; (DELETE "/api/session" json-auth/handle-session)
  ; (GET "/api/login" (fn [request] (redirect "../login.html")))
  ; (GET "/api/user-only-ping" (friend/wrap-authorize ping [::user]))
  ; (GET "/api/admin-only-ping" (friend/wrap-authorize ping [::admin]))
  )



(defresource session-resource

  ; :base (r/role-auth #{:user})
  :allowed-methods [:get :post :delete]
  :available-media-types ["application/json"]
  :malformed? (fn [ctx] (parse-body ctx :parsed-body))
  :handle-ok #((friend/current-authentication ))
  :handle-created nil
  :exists? (fn [ctx])
  :post! (fn [ctx]
    (let [username (ctx :parsed-body)
          password (ctx :parsed-body)]
          (if (not-any? str/blank? [username password])
            (let [user (create-user username password false)]
                ;; HERE IS WHERE YOU'D PUSH THE USER INTO YOUR DATABASES if desired
                (friend/merge-authentication (:request ctx)  user))))) ;workflow-result

  :delete!  nil)




(def site
  "Main handler for the example Compojure site."
  (-> site-routes
    (auth/friend-middleware users)
       ; (friend/requires-scheme-with-proxy :https)
       ; (ring-session/wrap-session)
       (api)))

;; ## Server Lifecycle

(defonce server (atom nil))

(defn kill! []
  (swap! server (fn [s] (when s (s) nil))))

(defn -main []
  (swap! server
         (fn [s]
           (if s
             (do (println "Server already running!") s)
             (do (println "Booting server on port 8090.")
                 (run-server (rl/wrap-reload #'site) {}))))))

(defn running?
  "Returns true if the server is currently running, false otherwise."
  []
  (identity @server))

(defn cycle!
  "Cycles the existing server - shut down the relaunch."
  []
  (kill!)
  (-main))
