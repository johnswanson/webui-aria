(ns webui-aria.constants
  (:require [schema.core :as s :include-macros true]))

(def successful 0)
(def unknown-error 1)
(def timeout 2)
(def not-found 3)
(def max-file-not-found 4)
(def speed-too-low 5)
(def network-error 6)
(def unfinished-downloads 7)
(def server-does-not-support-resume 8)
(def not-enough-disk-space 9)
(def piece-length-different 10)
(def downloading-same-file 11)
(def downloading-same-info-hash-torrent 12)
(def file-already-existed 13)
(def rename-failed 14)
(def could-not-open-existing 15)
(def could-not-create-new-file 16)
(def io-error 17)
(def could-not-create-directory 18)
(def name-resolution-failed 19)
(def could-not-parse-metalink 20)
(def ftp-failed 21)
(def bad-http-response-header 22)
(def too-many-redirects 23)
(def http-auth-failed 24)
(def could-not-parse-bencoded 25)
(def corrupted-torrent-file 26)
(def bad-magent-uri 27)
(def bad-option 28)
(def overloaded 29)
(def could-not-parse 30)
(def reserved-unused 31)
(def checksum-validation-failed 32)

(def error-enum (s/enum
                 successful
                 unknown-error
                 timeout
                 not-found
                 max-file-not-found
                 speed-too-low
                 network-error
                 unfinished-downloads
                 server-does-not-support-resume
                 not-enough-disk-space
                 piece-length-different
                 downloading-same-file
                 downloading-same-info-hash-torrent
                 file-already-existed
                 rename-failed
                 could-not-open-existing
                 could-not-create-new-file
                 io-error
                 could-not-create-directory
                 name-resolution-failed
                 could-not-parse-metalink
                 ftp-failed
                 bad-http-response-header
                 too-many-redirects
                 http-auth-failed
                 could-not-parse-bencoded
                 corrupted-torrent-file
                 bad-magent-uri
                 bad-option
                 overloaded
                 could-not-parse
                 reserved-unused
                 checksum-validation-failed))
