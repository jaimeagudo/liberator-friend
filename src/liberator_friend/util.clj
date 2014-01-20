(ns liberator-friend.util
  (:require [cheshire.core :refer :all]
    [clojure.java.io :as io]
    [liberator.dev :as dev]
    [cemerick.friend :as friend]
    [cemerick.friend.util :refer [gets]]
    (cemerick.friend [workflows :as workflows]
     [credentials :as creds])
    [clojure.data.json :as json]
    ; [paddleguru.util.liberator :as l]
    )
  (:use [liberator.core :only [defresource request-method-in]]
    [liberator.representation :only [Representation]]))

;; convert the body to a reader. Useful for testing in the repl
;; where setting the body to a string is much simpler.
(defn- body-as-string
  [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (parse-stream (io/reader body) true))))


(defn parse-body
"For PUT and POST parse the body as JSON and store in the context
 under the given key."
    [context key]
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (body-as-string context)]
        [false {key body}]
        {:message "No body"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: " (.getMessage e))}))))

;; For PUT and POST check if the content type is among defined types, usually JSON.
(defn check-content-type
  [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
     (some #{(get-in ctx [:request :headers "content-type"])}
           content-types)
     [false {:message "Unsupported Content-Type"}])
    true))

;  (:require [cemerick.friend :as friend]
;             [cemerick.friend.util :refer [gets]]
;             (cemerick.friend [workflows :as workflows]
;                              [credentials :as creds])
;             [clojure.data.json :as json]
;             [paddleguru.util.liberator :as l]))

; (defn login-failed
;   "called when JSON authentication fails."
;   [request]
;   (-> {:ok false
;        :reason "authentication failed"}
;       (l/json-response 401)))

; (defn read-json
;   "Parses json, guarding against the empty case."
;   [s]
;   (if-let [s (not-empty s)]
;     (json/read-json s)))

; ;; ## Public Methods

; (defn json-login
;   "json auth workflow implementation for friend."
;   [{:keys [uri request-method body] :as req}]
;   (when (and (= uri (:login-uri (::friend/auth-config req)))
;              (= :post request-method)
;              (= "application/json" (l/content-type req)))
;     (let [{:keys [username password] :as creds} (read-json (slurp body))
;           creds (with-meta creds {::friend/workflow :json-login})
;           cred-fn (:credential-fn (::friend/auth-config req))]
;       (if-let [user-record (and username password
;                                 (cred-fn creds))]
;         (workflows/make-auth user-record
;                              {::friend/workflow :json-login
;                               ::friend/redirect-on-auth? true})
;         (login-failed req)))))

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