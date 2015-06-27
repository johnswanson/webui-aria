(ns webui-aria.db
  (:require [schema.core :as s :include-macros true]
            [webui-aria.constants :as c]
            [webui-aria.schema.server :refer [->server]]
            [webui-aria.schema.uri :refer [->uri]]
            [webui-aria.schema.peer :refer [->peer]]
            [webui-aria.schema.file :refer [->file]]
            [webui-aria.schema.download :refer [->download Download]]
            [webui-aria.schema.connection :refer [->connection Connection]]))

(def Database
  {:connection Connection
   :downloads {s/Str Download}})

(def default-db
  {:connection (->connection {:token nil
                              :host "aria.mkn.io"
                              :port 6800
                              :secure? false
                              :path "/jsonrpc"})
   :requests {}
   :downloads {}})
