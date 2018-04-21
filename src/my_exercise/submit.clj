(ns my-exercise.submit
  (:require [clojure.java.shell :refer [sh] :as shell]
            [clojure.string :as str])
  (:gen-class))

(def zip-name "my-exercise.zip")

(defn prepare-project []
  (sh "lein" "clean")
  (println (:out (sh "zip" zip-name "-r" "."))))

(defn print-instructions []
  (println (str "We just zipped up your work into the file '" zip-name
                "' in this directory. Please attach it to an email to"
                " chris@democracy.works.")))

(defn -main [& args]
  (prepare-project)
  (print-instructions)
  (System/exit 0))
