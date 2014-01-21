(ns liberator-friend.core
  "Core namespace for the liberator-friend post."
  (:use [liberator-friend.users])
  (:require [cheshire.core :refer :all]
    [compojure.handler :refer [api]]
    [compojure.core :as compojure :refer (GET ANY defroutes)]
    [liberator-friend.middleware.auth :as auth]
    [liberator-friend.resources :as r :refer [defresource]]
    ; [clojure.contrib.string :as str]
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
  )


(def site
  "Main handler for the example Compojure site."
  (-> site-routes
    (auth/friend-middleware users)
       ; (friend/requires-scheme-with-proxy :https)
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
