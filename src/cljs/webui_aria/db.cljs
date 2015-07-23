(ns webui-aria.db
  (:require [schema.core :as s :include-macros true]
            [webui-aria.local-storage :as local-storage]
            [cljs.reader]))

(defn try-read [key]
  (try (cljs.reader/read-string (local-storage/get-item key))
       (catch js/Error _ nil)))

(def default-db
  {:connection (or (try-read :config)
                   {:token ""
                    :host "localhost"
                    :port 6800
                    :secure? false
                    :path "/jsonrpc"})
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
