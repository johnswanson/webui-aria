(ns webui-aria.schema.error
  (:require [webui-aria.coercion]
            [schema.coerce :as coerce]
            [schema.core :as s]))

(def Error s/Keyword)
