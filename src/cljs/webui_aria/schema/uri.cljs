(ns webui-aria.schema.uri
  (:require [schema.core :as s :include-macros true]
            [schema.coerce :as coerce]
            [webui-aria.coercion]))

(def Uri
  {:uri s/Str
   :status (s/enum :used :waiting)})

(def ->uri (coerce/coercer Uri webui-aria.coercion/matcher))
