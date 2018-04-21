(ns my-exercise.search
  (:require [hiccup.page :refer [html5]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [my-exercise.us-state :as us-state]
            [clojure.string :as s]
            [clj-http.client :as client]))
;;N.B. "curl 'https://api.turbovote.org/elections/upcoming?district-divisions=1'" does not fail fast
;;TODO what happens if something errors out? 404 setup
;;Request timeouts for turbovote api

(def TURBOVOTE-URL
  "https://api.turbovote.org/elections/upcoming")

(defn header [_]
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
   [:title "Your next election"]
   [:link {:rel "stylesheet" :href "default.css"}]])

(defn get-upcoming-elections [ocd-id]
  (client/get TURBOVOTE-URL {:query-params {"district-divisions" ocd-id}
                             :as :auto}))

(defn convert-address-to-ocd [{street :street street-2 :street-2 city :city state :state zip :zip}]
  (let [state-ocd (str "ocd-division/country:us/state:" (s/lower-case state))
        place-ocd (str state-ocd "/place:" (s/lower-case city))]
    (str state-ocd "," place-ocd)))

(defn results [request]
  [:div {:class "instructions"}])

(defn page [request]
  (println (:params request))
  (html5
   (header request)
   (results request)))

(def example-data
  {:city "Newark"
   :state "NJ"
   :street "303 Fleming Road"
   :street-2 "" 
   :zip "45215"})

(defn do-test []
  (get-upcoming-elections (convert-address-to-ocd example-data))
  )
