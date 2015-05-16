(ns webui-aria.handlers
  (:require [webui-aria.db :refer [default-value]]
            [webui-aria.api :as api]
            [webui-aria.api-defaults :as api-defaults]
            [re-frame.core :refer [register-handler path trim-v after]]))

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

