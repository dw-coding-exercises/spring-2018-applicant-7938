(ns my-exercise.search
  (:require [hiccup.page :refer [html5]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [my-exercise.us-state :as us-state]
            [clojure.string :as s]
            [clj-http.client :as client]
            [clojure.pprint :as pprint]))

;;Notes
;;N.B. "curl 'https://api.turbovote.org/elections/upcoming?district-divisions=1'" does not fail fast
;;TODO what happens if something errors out? 404 setup
;;Request timeouts for turbovote api

;;Constants 
(def TURBOVOTE-URL
  "https://api.turbovote.org/elections/upcoming")

;; Data marshalling 
(defn get-upcoming-elections
  "Submit the ocd-id to the TurboVote API and get back the results."
  [ocd-id]
  (client/get TURBOVOTE-URL {:query-params {"district-divisions" ocd-id}
                             :as :auto}))

(defn convert-address-to-ocd
  "Converts the address submitted via query parameters of the POST request and
  converts it into the corresponding OCD-ID's for the state and city."
  [{street :street street-2 :street-2 city :city state :state zip :zip}]
  (let [state-ocd (str "ocd-division/country:us/state:" (s/lower-case state))
        city (-> city (s/replace " " "_") s/lower-case)
        place-ocd (str state-ocd "/place:" city)]
    (str state-ocd "," place-ocd)))

;; Display
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

;;Development vars
(def example-data
  {:city "Newark Test"
   :state "NJ"
   :street "303 Fleming Road"
   :street-2 "" 
   :zip "45215"})

(defn do-test []
  (get-upcoming-elections (convert-address-to-ocd example-data)))
