(ns webui-aria.db
  (:require [schema.core :as s :include-macros true]))

(def default-db
  {:connection {:token nil
                :host "aria.mkn.io"
                :port 6800
                :secure? false
                :path "/jsonrpc"}
   :api-connection-status :disconnected
   :requests {}
   :downloads {}
   :filters {:active   true
             :waiting  true
             :paused   true
             :error    true
             :complete true
             :removed  true
             :linked   false}})
