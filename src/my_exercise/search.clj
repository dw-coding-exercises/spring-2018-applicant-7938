(ns my-exercise.search
  (:require [hiccup.page :refer [html5]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [my-exercise.us-state :as us-state]
            [clojure.string :as s]
            [clj-http.client :as client]))

;;;;;;;;;;;
;;Notes/TODO's
;;;;;;;;;;;

;;N.B. "curl 'https://api.turbovote.org/elections/upcoming?district-divisions=1'" does not fail fast
;;TODO what happens if something errors out? 404 setup and displaying a helpful error to the user
;;TODO Request timeouts for turbovote api
;;TODO Create a clojure.spec for OCD 
;;TODO If anything goes wrong al all, then the entire web app freaks out and null pointer exceptions win the day.

;;Constants 
(def TURBOVOTE-URL
  "https://api.turbovote.org/elections/upcoming")

(def GOOGLE-MAPS-GEOCODING-URL
  "https://maps.googleapis.com/maps/api/geocode/json")

;;This is bad to do!!! But not really any other options in terms of secret management and coordiation right now.
(def GOOGLE-MAPS-GEOCODING-API-KEY
  "AIzaSyC3T4g-5LkOIcb0fJPuOoMMOQMViRYa3Sw")

;;;;;;;;;;;
;; Data marshalling 
;;;;;;;;;;;

(defn get-county-of-address
  "Takes in an address, sends the data over to google, parses the results out and
  turns it into an ocd style county name."
  [{street :street street-2 :street-2 city :city state :state zip :zip}]
  (let [address (s/join ", " (filter (complement empty?) [street street-2 city state]))
        result (client/get GOOGLE-MAPS-GEOCODING-URL
                           {:query-params {"address" address
                                           "key" GOOGLE-MAPS-GEOCODING-API-KEY}
                            :as :json})
        address-components (-> result :body :results first :address_components)
        county-name (->> address-components
                         (filter #(= (:types %) ["administrative_area_level_2" "political"]))
                         first
                         :long_name)
        ocd-county-name (-> county-name
                        (s/replace "County" "")
                        s/trim
                        (s/replace " " "_")
                        s/lower-case)]
    ocd-county-name))

(defn get-upcoming-elections
  "Submit the ocd-id to the TurboVote API and get back the results."
  [ocd-id]
  (client/get TURBOVOTE-URL {:query-params {"district-divisions" ocd-id}
                             :as :auto}))

(defn convert-address-to-ocd
  "Converts the address submitted via query parameters of the POST request and
  converts it into the corresponding OCD-ID's for the state and city."
  [{street :street street-2 :street-2 city :city state :state zip :zip :as m}]
  (let [state-ocd (str "ocd-division/country:us/state:" (s/lower-case state))
        city (-> city (s/replace " " "_") s/lower-case)
        place-ocd (str state-ocd "/place:" city)
        ocd-county-name (get-county-of-address m)
        county-ocd (str state-ocd "/county:" ocd-county-name)]
    (str state-ocd "," place-ocd "," county-ocd)))


;;;;;;;;;;;
;; Display
;;;;;;;;;;;

(defn header
  "Provide a simple HTML header for the page."
  [_]
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
   [:title "Your next election"]
   [:link {:rel "stylesheet" :href "default.css"}]])


(defn display-request
  "A simple descrpition for the top of the page that reminds the user what the
  page is about and what we thought they submitted as their address."
  [{{street :street street-2 :street-2 city :city state :state zip :zip} :params}]
  [:div {:class ""}
   [:h1 "Upcoming Elections for Your Address"]
   "Submitted Address: " (s/join ", " (filter (complement empty?) [street street-2 city state]))])

(defn display-results
  "Given the results of the query to TurboVote, returns an extremely display of
  the upcoming election. Not much design or thought put into it so far, depends
  on what information we want to have highlighted and what actions we would like
  the user to take."
  [results]
  [:div {:class ""}
   (for [{description :description
          website :website
          polling-place-url :polling-place-url
          date :date}
         results]
     [:div {:class "upcoming-election"}
      [:a {:href website} description]
      date ;; TODO make this easier to read when displayed
      [:a {:href polling-place-url} "Polling Place"]])])

(defn page
  "Main function that is called by core. Takens in a POST request with query
  params contained an address submitted by the user and returns a page with the
  upcoming elections for that address."
  [request]
  (let [results (-> request :params convert-address-to-ocd get-upcoming-elections :body)]
    (html5
     (header request)
     (display-request request)
     (display-results results))))

;;;;;;;;;;;
;;Development vars
;;;;;;;;;;;

(def example-data
  {:city "Wyoming"
   :state "OH"
   :street "303 Fleming Road"
   :street-2 "" 
   :zip "45215"})

(defn do-test []
  (get-upcoming-elections (convert-address-to-ocd example-data)))
