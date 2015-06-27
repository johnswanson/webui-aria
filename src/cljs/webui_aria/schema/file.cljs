(ns webui-aria.schema.file
  (:require [schema.core :as s :include-macros true]
            [schema.coerce :as coerce]
            [webui-aria.coercion]
            [webui-aria.schema.uri :refer [Uri]]))

(def File
  {:index s/Int
   :path s/Str
   :length s/Int
   :completed-length s/Int
   :selected s/Bool
   :uris [Uri]})

(def ->file (coerce/coercer File webui-aria.coercion/matcher))
