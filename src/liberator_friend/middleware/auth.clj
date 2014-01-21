(ns liberator-friend.middleware.auth
  (:require
    [ring.middleware.keyword-params :as params]
     [cheshire.core :refer :all]
[liberator-friend.util]
     ; [clojure.data.json :as json]
    [cemerick.friend :as friend]
    (cemerick.friend [workflows :as workflows]
                      [credentials :as creds]))
  (:use [liberator-friend.util]))


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

(defn read-json
  "Parses json, guarding against the empty case."
  [s]
  (if-let [s (not-empty s)]
     (parse-string s)))
      ; (json/read-json s)))


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
  [& {:keys [login-uri credential-fn login-failure-handler redirect-on-auth?] :as form-config
      :or {redirect-on-auth? true}}]
      (fn   [{:keys [uri request-method body] :as req}]
  (when (and (= uri (:login-uri (::friend/auth-config req)))
             (= :post request-method)
             ; (= "application/json" (l/content-type req))
             )
    (let [{:keys [username password] :as creds}  (parse-json-body body) ; (read-json (slurp body))
          creds (with-meta creds {::friend/workflow :json-login})
          cred-fn (:credential-fn (::friend/auth-config req))]
      (if-let [user-record (and username password
                                (cred-fn creds))]
        (workflows/make-auth user-record
                             {::friend/workflow :json-login
                              ::friend/redirect-on-auth? true})
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
  (let [friend-m {:credential-fn (partial creds/bcrypt-credential-fn users)
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
        (friend/authenticate friend-m)
        ; (friend/requires-scheme-with-proxy :https)
        )))


; ;; curl -d '{"username": "jaime", "password": "password"}' -H "Content-Type:application/json" -i http://localhost:8090/login
; ;; If redirect is set to true,

; HTTP/1.1 303 See Other
; Set-Cookie: ring-session=session%3Acc18a113-2798-4673-9a84-82af52e82bef;Path=/
; Location: /profile
; Content-Length: 0
; Server: http-kit
; Date: Tue, 19 Nov 2013 20:37:09 GMT

; ;; If it's set to false,
; HTTP/1.1 200 OK
; Content-Type: application/json
; Content-Length: 95
; Server: http-kit
; Date: Tue, 19 Nov 2013 20:38:27 GMT

; {"ok":true,"reason":"Authentication succeeded! Save the cookie returned in the cookie header."}