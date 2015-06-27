(ns webui-aria.schema.connection
  (:require [schema.core :as s :include-macros true]
            [schema.coerce :as coerce]
            [webui-aria.coercion]))

(def Connection
  {:token (s/maybe s/Str)
   :host s/Str
   :port s/Int
   :secure? s/Bool
   :path s/Str})

(def ->connection
  (coerce/coercer Connection webui-aria.coercion/matcher))
