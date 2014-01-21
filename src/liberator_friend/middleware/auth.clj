(ns liberator-friend.middleware.auth
  (:require
    ; [ring.middleware.keyword-params :as params]
    [cheshire.core :refer :all]
    [clojure.java.io :as io]
    [cemerick.friend :as friend]
    (cemerick.friend [workflows :as workflows]
                     [credentials :as creds]))
  (:use [ring.middleware.session]
   [liberator-friend.util]))

(defn login-failed
  "called when JSON authentication fails."
  [request]
  (-> {:ok false
       :reason "authentication failed"}
(generate-string 401)))
            ; (l/json-response 401)))

; (defn json-workflow
;   [& {:keys [login-uri credential-fn login-failure-handler redirect-on-auth?] :as auth-config
;       :or {redirect-on-auth? true}}]
;       (fn   [{:keys [uri request-method body] :as req}]
;   (when (and (= uri (:login-uri (::friend/auth-config req)))
;              (= :post request-method)
;              (= "application/json" (:content-type req)))
;     (let [{:keys [username password] :as creds} (parse-json-stream body)
;           creds (with-meta creds  {::friend/workflow :json-login})
;           cred-fn (:credential-fn (::friend/auth-config req))]
;       (if-let [user-record (and username password
;                                 (cred-fn creds))]
;         ;(do (trace "user logged in")
;         (workflows/make-auth user-record
;                              {::friend/workflow :json-login
;                               ::friend/redirect-on-auth? redirect-on-auth?})
;         (login-failed req))))))




(defn json-workflow
  [& {:keys [login-uri credential-fn login-failure-handler redirect-on-auth?] :as auth-config
      :or {redirect-on-auth? true}}]
      (fn   [{:keys [uri request-method body] :as req}]
       (condp = uri
        (:login-uri (::friend/auth-config req))
        (if (and (= :post request-method)
                 (= "application/json" (:content-type req)))
        (let [{:keys [username password] :as creds} (parse-json-stream body)
              creds (with-meta creds  {::friend/workflow :json-login})
              cred-fn (:credential-fn (::friend/auth-config req))]
            (if-let [user-record (and username password (cred-fn creds))]
                    ;(do (trace "user logged in")
                      (workflows/make-auth user-record
                       {::friend/workflow :json-login
                        ::friend/redirect-on-auth? redirect-on-auth?})
                      (login-failed req))))
        (:logout-uri (::friend/auth-config req))
        (friend/logout*  {:status 200})
        nil)
    ))


(defn friend-middleware
  "Returns a middleware that enables authentication via Friend."
  [handler users]
  (let [auth-config {:credential-fn (partial creds/bcrypt-credential-fn users)
                     :redirect-on-auth? false
                     :logout-uri "/logout"
                     :workflows
                  ;; Note that ordering matters here. Basic first.
                  [
                   ; (workflows/http-basic :realm "/") disabled for
                   ; (workflows/interactive-form)
                   (json-workflow)
                   ]}]
     ; (derive ::admin ::user)
    (-> handler
        (friend/authenticate auth-config)
          (wrap-session)
          ; (friend/requires-scheme-with-proxy :https)
        )))



; Booting server on port 8090.
; {:remote-addr "127.0.0.1", :scheme :http, :query-params {}, :session {}, :cemerick.friend/auth-config {:redirect-on-auth? false, :default-landing-uri "/", :login-uri "/login", :credential-fn #<core$partial$fn__4190 clojure.core$partial$fn__4190@48e873b1>, :workflows [#<auth$json_workflow$fn__4664 liberator_friend.middleware.auth$json_workflow$fn__4664@1c98184b>]}, :form-params {}, :request-method :post, :query-string nil, :content-type "application/json", :cookies {}, :websocket? false, :async-channel #<AsyncChannel /127.0.0.1:8090<->/127.0.0.1:45976>, :uri "/login", :session/key nil, :server-name "localhost", :params {}, :headers {"accept" "*/*", "content-length" "45", "content-type" "application/json", "host" "localhost:8090", "user-agent" "curl/7.29.0"}, :content-length 45, :server-port 8090, :character-encoding "utf8", :body #<BytesInputStream BytesInputStream[len=45]>}
; {:username "jaime", :roles #{:user}}


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
; $curl -b galletas -i http://localhost:8090/logout
