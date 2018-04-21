(ns my-exercise.search
  (:require [hiccup.page :refer [html5]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [my-exercise.us-state :as us-state]
            [clojure.string :as s]
            [clj-http.client :as client]
            [clojure.pprint :as pprint]))
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
        city (-> city (s/replace " " "_") s/lower-case)
        place-ocd (str state-ocd "/place:" city)]
    (str state-ocd "," place-ocd)))

(defn display-request [{{street :street street-2 :street-2 city :city state :state zip :zip} :params}]
  [:div {:class ""}
   [:h1 "Upcoming Elections for Your Address"]
   "Submitted Address: " (s/join "," [street street-2 city state])
   ]
  )

;;Extremely simple display of results, not much design put into it so far.
;;Depends on what we want the user to do after this looking all this information
;;up, as well as what information we want to highlight.

(defn display-results [results]
  [:div {:class ""}
   (for [{description :description
          website :website
          polling-place-url :polling-place-url
          date :date}
         results]
     [:div {:class "upcoming-election"}
      [:a {:href website} description]
      date ;; TODO make this easier to read when displayed
      ])
   
   ])

(defn page [request]
  (let [results (-> request :params convert-address-to-ocd get-upcoming-elections :body)]
    (pprint/pprint results)
    (html5
     (header request)
     (display-request request)
     (display-results results))))

(def example-data
  {:city "Newark Test"
   :state "NJ"
   :street "303 Fleming Road"
   :street-2 "" 
   :zip "45215"})

(defn do-test []
  (get-upcoming-elections (convert-address-to-ocd example-data))
  )
