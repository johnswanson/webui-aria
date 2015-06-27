(ns webui-aria.schema.download
  (:require [schema.core :as s :include-macros true]
            [schema.coerce :as coerce]
            [webui-aria.schema.file :refer [File]]
            [webui-aria.coercion]
            [webui-aria.constants :as c]))

(def Download
  {:status (s/enum :active :waiting :paused :error :complete :removed)
   :gid s/Str
   :total-length s/Int
   :completed-length s/Int
   :upload-length s/Int
   (s/optional-key :bitfield) s/Str
   :download-speed s/Int
   :upload-speed s/Int
   (s/optional-key :info-hash) s/Str
   (s/optional-key :num-seeders) s/Int
   :piece-length s/Int
   :num-pieces s/Int
   :connections s/Int
   :error-code c/error-enum
   (s/optional-key :followed-by) [s/Str]
   (s/optional-key :belongs-to) s/Str
   :dir s/Str
   :files [File]
   (s/optional-key :bittorrent) {:announce-list [[s/Str]]
                                 :comment s/Str
                                 :creation-date s/Int
                                 :mode (s/enum :single :multi)
                                 :info {:name s/Str}}})

(def ->download (coerce/coercer Download webui-aria.coercion/matcher))
