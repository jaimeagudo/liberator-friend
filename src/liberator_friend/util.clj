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


(defmacro parse-json-stream
  [s]
  ; (condp instance? body
  ;     java.lang.String body
  `(parse-stream (io/reader ~s) true))
; (parse-stream (io/reader s)))


(defn parse-json-body
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
      (if-let [body (parse-json-body context)]
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

