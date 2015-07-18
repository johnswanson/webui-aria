(ns webui-aria.schema.download
  (:require [schema.core :as s :include-macros true]
            [clojure.string :as str]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [schema.coerce :as coerce]))

(defn ->keyword [thing]
  (->kebab-case-keyword thing))

(defn string->boolean [s]
  (if (string? s)
    (= "true" (str/lower-case s))
    s))

(defn string->uuid [s]
  (if (string? s)
    (cljs.core.UUID. s)
    s))

(defn string->int [s]
  (if (string? s)
    (js/parseInt s 10)
    s))

(defn set-matcher [schema]
  (if (instance? cljs.core.PersistentHashSet schema)
    (fn [x] (if (sequential? x) (set x) x))))

(defn keyword-enum-matcher [schema]
  (when (and (instance? s/EnumSchema schema)
             (every? keyword? (.-vs ^schema.core.EnumSchema schema)))
    ->keyword))

(def error-codes
  {:successful 0
   :unknown-error 1
   :timeout 2
   :not-found 3
   :max-file-not-found 4
   :speed-too-low 5
   :network-error 6
   :unfinished-downloads 7
   :server-does-not-support-resume 8
   :not-enough-disk-space 9
   :piece-length-different 10
   :downloading-same-file 11
   :downloading-same-info-hash-torrent 12
   :file-already-existed 13
   :rename-failed 14
   :could-not-open-existing 15
   :could-not-create-new-file 16
   :io-error 17
   :could-not-create-directory 18
   :name-resolution-failed 19
   :could-not-parse-metalink 20
   :ftp-failed 21
   :bad-http-response-header 22
   :too-many-redirects 23
   :http-auth-failed 24
   :could-not-parse-bencoded 25
   :corrupted-torrent-file 26
   :bad-magent-uri 27
   :bad-option 28
   :overloaded 29
   :could-not-parse 30
   :reserved-unused 31
   :checksum-validation-failed 32})

(def error-kw
  (into {} (map (fn [[a b]] [b a]) error-codes)))

(def Error (apply s/enum (keys error-codes)))


(def coercions
  {s/Keyword ->keyword
   s/Bool    string->boolean
   s/Uuid    string->uuid
   s/Int     string->int
   Error     #(error-kw (js/parseInt % 10))})

(defn matcher [schema]
  (or (coercions schema)
      (keyword-enum-matcher schema)
      (set-matcher schema)))

(def BittorrentData {(s/optional-key :comment)       s/Str
                     (s/optional-key :creation-date) s/Int
                     (s/optional-key :mode)          (s/enum :single :multi)
                     (s/optional-key :info)          {:name s/Str}
                     (s/optional-key :announce-list) [[s/Str]]})

(def File
  {:index            s/Int
   :path             s/Str
   :length           s/Int
   :completed-length s/Int
   :selected         s/Bool
   :uris             [{:uri    s/Str
                       :status (s/enum :used :waiting)}]})

(def Status (s/enum :active :waiting :paused :error :complete :removed :linked))

(def Download
  {:status                       Status
   :gid                          s/Str
   :total-length                 s/Int
   :completed-length             s/Int
   :upload-length                s/Int
   :download-speed               s/Int
   :upload-speed                 s/Int
   :piece-length                 s/Int
   :num-pieces                   s/Int
   :connections                  s/Int
   :dir                          s/Str
   :files                        [File]
   (s/optional-key :bitfield)    s/Str
   (s/optional-key :info-hash)   s/Str
   (s/optional-key :num-seeders) s/Int
   (s/optional-key :error-code)  Error
   (s/optional-key :followed-by) [s/Str]
   (s/optional-key :belongs-to)  s/Str
   (s/optional-key :bittorrent)  BittorrentData})

(def coercer (coerce/coercer Download matcher))

(defn ->download [d]
  (coercer (if (:followed-by d)
             (assoc d :status :linked)
             d)))

