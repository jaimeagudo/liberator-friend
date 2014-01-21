(ns liberator-friend.middleware.auth
  (:require
    [ring.middleware.keyword-params :as params]
    [cheshire.core :refer :all]
    [liberator-friend.util]
    [clojure.java.io :as io]
    ; [clojure.data.json :as json]
    [cemerick.friend :as friend]
    (cemerick.friend [workflows :as workflows]
                     [credentials :as creds]))
  (:use [ring.middleware.session]
   [liberator-friend.util]))

; (use 'ring.middleware.session
     ; 'ring.util.response)

; (defn handler [{session :session}]
  ; (response (str "Hello " (:username session))))

; (def app

;

; (defn authenticate-user
; [{username "username" password "password"}]
;  ; [{username "username" password "password"}]

;   (if-let [user-record (get-user-for-username username)]
;     (if (creds/bcrypt-verify password (:password user-record))
;       (dissoc user-record :password))))



; ;; ## Friend Middleware
; (defn json-login-workflow []
;   (routes
;     (GET "/logout" req
;       (friend/logout* {:status 200}))
;     (POST "/login" {body :body}
;       (if-let [user-record (authenticate-user body)]
;         (workflows/make-auth user-record {:cemerick.friend/workflow :json-login-workflow})
;         {:status 401}))))


(defn login-failed
  "called when JSON authentication fails."
  [request]
  (-> {:ok false
       :reason "authentication failed"}
(generate-string 401)))
            ; (l/json-response 401)))

; (defn read-json
;   "Parses json, guarding against the empty case."
;   [s]
;   (if-let [s (not-empty s)]
;      (parse-string s)))
;       ; (json/read-json s)))


; (defn json-login
;   "json auth workflow implementation for friend."
;   [{:keys [uri request-method body] :as req}]
;   (when (and (= uri (:login-uri (::friend/auth-config req)))
;              (= :post request-method)
;              ; (= "application/json" (l/content-type req))
;              )
;     (let [{:keys [username password] :as creds} (read-json (slurp body))
;           creds (with-meta creds {::friend/workflow :json-login})
;           cred-fn (:credential-fn (::friend/auth-config req))]
;       (if-let [user-record (and username password
;                                 (cred-fn creds))]
;         (workflows/make-auth user-record
;                              {::friend/workflow :json-login
;                               ::friend/redirect-on-auth? true})
;         (login-failed req)))))


(defn json-workflow
  [& {:keys [login-uri credential-fn login-failure-handler redirect-on-auth?] :as auth-config
      :or {redirect-on-auth? true}}]
      (fn   [{:keys [uri request-method body] :as req}]
  (when (and (= uri (:login-uri (::friend/auth-config req)))
             (= :post request-method)
             ; (= "application/json" (l/content-type req))
             )
; (prn (parse-stream (io/reader body) true))
        ; (prn (:body (parse-json-body body))
    (let [{:keys [username password] :as creds} (parse-json-stream body)
           ; (parse-json-body body) ; (read-json (slurp body))
          creds (with-meta creds {::friend/workflow :json-login})
          cred-fn (:credential-fn (::friend/auth-config req))]
      (prn (and username password
                                (cred-fn creds)))
      (if-let [user-record (and username password
                                (cred-fn creds))]

        (workflows/make-auth user-record
                             {::friend/workflow :json-login
                              ::friend/redirect-on-auth? redirect-on-auth?})
        (login-failed req))))))


; ;;; original friend workflow
; (defn interactive-form
;   [& {:keys [login-uri credential-fn login-failure-handler redirect-on-auth?] :as form-config
;       :or {redirect-on-auth? true}}]
;   (fn [{:keys [request-method params] :as request}]
;     (when (and (= (gets :login-uri form-config (::friend/auth-config request)) (req/path-info request))
;                (= :post request-method))
;       (let [{:keys [username password] :as creds} (select-keys params [:username :password])]
;         (if-let [user-record (and username password
;                                   ((gets :credential-fn form-config (::friend/auth-config request))
;                                     (with-meta creds {::friend/workflow :interactive-form})))]
;           (make-auth user-record
;                      {::friend/workflow :interactive-form
;                       ::friend/redirect-on-auth? redirect-on-auth?})
;           ((or (gets :login-failure-handler form-config (::friend/auth-config request)) #'interactive-login-redirect)
;             (update-in request [::friend/auth-config] merge form-config)))))))


; (defn friend-json-middleware
;   [handler users]
;   (let [friend-m  {:workflows [
;                               (auth-workflow)
;                               ]}]
;     (-> handler
;         (friend/authenticate friend-m)
;         ; (friend/requires-scheme-with-proxy :https)
;         (params/wrap-keyword-params)
;         (json/wrap-json-body)
;         (json/wrap-json-response {:pretty true})

;         ) ))


; (def app
;   (-> (handler/site
;         (friend/authenticate app-routes
;           {:workflows [(authentication-workflow)]}))
;       (params/wrap-keyword-params)
;       (json/wrap-json-body)
;       (json/wrap-json-response {:pretty true})))


(defn friend-middleware
  "Returns a middleware that enables authentication via Friend."
  [handler users]
  (let [auth-config {:credential-fn (partial creds/bcrypt-credential-fn users)
                     :redirect-on-auth? false
                  :workflows
                  ;; Note that ordering matters here. Basic first.
                  [
                   ; (workflows/http-basic :realm "/") disabled for cently
                   ;; The tutorial doesn't use this one, but you
                   ;; probably will.
                   ; (workflows/interactive-form)
                   (json-workflow)
                   ; (json-login-workflow)
                   ]}]
    (-> handler

        (friend/authenticate auth-config)
          (wrap-session)
 ; (friend/requires-scheme-with-proxy :https)
        )))






; $ curl -c galletas -d '{"username": "jaime", "password": "password"}' -H "Content-Type:application/json" -i http://localhost:8090/login
; HTTP/1.1 303 See Other
; Set-Cookie: ring-session=1e76c828-e08a-4656-aae3-074ec1262a61;Path=/
; Location: /
; Content-Length: 0
; Server: http-kit
; Date: Tue, 21 Jan 2014 19:31:01 GMT

; $ curl -b galletas -i http://localhost:8090/user
; HTTP/1.1 200 OK
; Vary: Accept
; Content-Type: application/json;charset=UTF-8
; Content-Length: 16
; Server: http-kit
; Date: Tue, 21 Jan 2014 19:31:07 GMT


; "Welcome, user!

; $ curl -b galletas -i http://localhost:8090/admin
; HTTP/1.1 401 Unauthorized
; Content-Type: application/json;charset=UTF-8
; Content-Length: 45
; Server: http-kit
; Date: Tue, 21 Jan 2014 19:33:18 GMT

; {"success":false,"message":"Not authorized!"}