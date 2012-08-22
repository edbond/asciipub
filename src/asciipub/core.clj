(ns asciipub.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]
            [clj-http.client :as client])
  (:use clojure.java.io
        clojure.repl)
  (:import [nl.siegmann.epublib.domain Author Book Resource TOCReference]
           [nl.siegmann.epublib.epub EpubWriter]
           [nl.siegmann.epublib.service MediatypeService]
           [java.io File FileOutputStream]))

(def cast-url "http://asciicasts.com/episodes/326-activeattr")

(defn url-to-fn
  ([url]
     {:post [(= false (nil? %))]}
     (last (re-find #"/([^/]+)$" url)))  
  ([url ext]
     (s/join "." [(url-to-fn url) ext])))

(def html-header
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" 
   \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">
 <html xmlns=\"http://www.w3.org/1999/xhtml\">
<head>
<title>Asciicasts</title>
</head>
<body>")

(def html-footer
  "</body></html>")

(defn local-resource
  "Creates Resource from filename. custom *name* may be given"
  ([filename]
     (Resource. (-> filename resource
                    input-stream) filename))
  ([filename name]
     ;; (prn filename name)
     (Resource. (-> filename resource
                    input-stream) name)))

(defn localize-resource
  "Download remote resource and add it to book"
  [book directory node]
  (let [src (-> node :attrs :src)
        imgfilename (url-to-fn src)
        img (-> (client/get src {:as :byte-array}) :body)
        target (s/join "/" ["resources" directory imgfilename])
        relfilename (s/join "/" [directory imgfilename])]
    ;; (prn src directory imgfilename)
    (with-open [w (java.io.FileOutputStream. target)]
      (.write w img))
    (prn imgfilename target)
    ;; add image Resource to Book
    (.addResource book (local-resource relfilename imgfilename))
    (assoc-in node [:attrs :src] imgfilename)))

(defn load-html
  [book directory link]
  (let [html
        (-> link java.net.URL. html/html-resource
            (html/select [:div.wrapper])
            (html/at
             [:h2 :> :*] nil            ; remove google plus tag
             [:p#otherFormats] nil      ; remove other formats
             [:.episodeLinks] nil
             [:img] (fn [node] (localize-resource book directory node))) ; remove next/prev links at the bottom
            (html/emit*))
        full-html (str html-header (apply str html) html-footer)]
    (spit "test.html" full-html)
    full-html))

(defn html-resource
  [book url]
  (let [reldir (url-to-fn url)
        directory (s/join "/" ["resources" (url-to-fn url)])
        filename (url-to-fn url "html")
        relative (s/join "/" [(url-to-fn url) filename])
        full-filename (s/join "/" [directory filename])
        ;; Load and clean html
        content (load-html book reldir url)]
    (-> directory File. .mkdirs)        ; create directories
    (spit full-filename content)        ; dump html content to file
    (local-resource relative filename)))

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
    (.addSection book "Introduction" (html-resource book cast-url))
    (.write writer book (output-stream "test.epub"))))

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))
