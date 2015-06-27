(ns webui-aria.schema.server
  (:require [schema.core :as s :include-macros true]
            [schema.coerce :as coerce]
            [webui-aria.coercion]))

(def Server
  {:uri s/Str
   :current-uri s/Str
   :download-speed s/Int})

(def ->server (coerce/coercer Server webui-aria.coercion/matcher))
