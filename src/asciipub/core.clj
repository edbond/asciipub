(ns asciipub.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s])
  (:use clojure.java.io
        clojure.repl)
  (:import [nl.siegmann.epublib.domain Author Book Resource TOCReference]
           [nl.siegmann.epublib.epub EpubWriter]
           [nl.siegmann.epublib.service MediatypeService]
           [java.io File]))

(def cast-url "http://asciicasts.com/episodes/326-activeattr")

(defn url-to-fn
  ([url]
     {:post [(= false (nil? %))]}
     (last (re-find #"/([^/]+)$" url)))  
  ([url ext]
     (s/join "." [(url-to-fn url) ext])))

(defn load-html
  [link]
  (let [url (java.net.URL. link)]
      (-> (html/html-resource url)
          (html/select [:div.wrapper])
          first
          (html/emit*)
          (->> (apply str)))))

(defn html-resource
  [url]
  (let [directory (s/join "/" ["resources" (url-to-fn url)])
        filename (url-to-fn url "html")
        relative (s/join "/" [(url-to-fn url) filename])
        full-filename (s/join "/" [directory filename])
        content (load-html url)]
    
    (-> directory File. .mkdirs)
    (spit full-filename content)
    (local-resource relative filename)))

(defn local-resource
  ([filename]
     (Resource. (-> filename resource
                    input-stream) filename))
  ([filename name]
     (prn filename name)
     (Resource. (-> filename resource
                    input-stream) name)))

(defn create-book
  []
  (let [book (Book.)
        md (.getMetadata book)
        author (Author. "Eduard" "Bondarenko")
        writer (EpubWriter.)]
    (doto md
      (.addTitle "Asciicasts")
      (.addAuthor author))
    (.setCoverImage book (local-resource "asciicasts.png"))
    (.addSection book "Introduction" (html-resource cast-url))
    (.write writer book (output-stream "test.epub"))))

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))
