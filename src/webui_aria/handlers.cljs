(ns webui-aria.handlers
  (:require [webui-aria.db :refer [default-value]]
            [webui-aria.api :as api]
            [webui-aria.api-defaults :as api-defaults]
            [re-frame.core :refer [register-handler path trim-v after dispatch]]))

(register-handler
 :init-db
 (fn [_ _]
   default-value))

(register-handler
 :filter-toggled
 [(path :filters) trim-v]
 (fn [filters [fname]]
   (update-in filters [fname :enabled?] not)))

(register-handler
 :api-connect
 [(path :api) trim-v]
 (fn [api [config]]
   (api/api (merge api-defaults/defaults config))))

(register-handler
 :api-get-all
 [(path :api) trim-v]
 (fn [api]
   (api/get-active api)
   (api/get-waiting api 0 10000)
   (api/get-stopped api 0 10000)
   api))

(register-handler
 :download-status-received
 [(path :downloads) trim-v]
 (fn [downloads [gid status]]
   (assoc downloads gid status)))

