(ns webui-aria.schema.peer
  (:require [schema.core :as s :include-macros true]
            [schema.coerce :as coerce]
            [webui-aria.coercion]))

(def Peer
  {:peer-id        s/Str
   :ip             s/Str
   :port           s/Str
   :bitfield       s/Str
   :am-choking     s/Bool
   :peer-choking   s/Bool
   :download-speed s/Int
   :upload-speed   s/Int
   :seeder         s/Bool})

(def ->peer (coerce/coercer Peer webui-aria.coercion/matcher))
