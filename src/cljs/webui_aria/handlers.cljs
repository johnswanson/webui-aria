(ns webui-aria.handlers
  (:require [re-frame.core :as re-frame]
            [re-frame.utils :refer [error warn log]]
            [cljs-uuid-utils.core :as uuid]
            [webui-aria.db :as db]
            [webui-aria.api :as api]
            [webui-aria.schema.download :refer [->download]]))

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(defn register-api-handler [call-name]
  (re-frame/register-handler
   call-name
   [re-frame/trim-v]
   (fn [db [args]]
     (let [config (:connection db)
           request (api/request call-name config args)
           id (:id request)]
       (api/request! config request)
       (update-in db [:pending-requests id]
                  assoc
                  :config config
                  :id id
                  :method call-name
                  :request request)))))

(defn apply-status-updates [db statuses]
  (reduce (fn [db download]
            (let [dl (->download download)]
              (if (:error dl)
                (do (error "failed to coerce download!" (:error dl)) db)
                (update-in db [:downloads (:gid dl)] merge dl))))
          db
          statuses))

(def api-handlers
  {:add-uri
   identity
   :get-status
   identity
   :tell-active
   (fn [db {[& statuses] :result} _]
     (apply-status-updates db statuses))
   :remove
   identity
   :force-remove
   identity
   :unpause
   identity
   :unpause-all
   identity
   :pause
   identity
   :pause-all
   identity
   :force-pause
   identity
   :force-pause-all
   identity
   :tell-waiting
   (fn [db {[& statuses] :result} _]
     (apply-status-updates db statuses))
   :tell-stopped
   (fn [db {[& statuses] :result} _]
     (apply-status-updates db statuses))
   nil
   (fn [db response req]
     (error "response received, no handler found")
     (error response)
     (error req)
     db)})

(doseq [api-handler (keys api-handlers)]
  (when api-handler
    (register-api-handler api-handler)))

(defn handler [method]
  (fn [db response request]
    (-> ((api-handlers method) db response request)
        (update-in [:pending-requests] dissoc (:id response)))))

(re-frame/register-handler
 :api-response-received
 [re-frame/trim-v]
 (fn [db [{:keys [id] :as response}]]
   (let [request (get-in db [:pending-requests id])]
     ((handler (:method request)) db response request))))

(re-frame/register-handler
 :api-message-received
 [re-frame/trim-v]
 (fn [db [message]]
   (api/on-message-received message)
   db))

(re-frame/register-handler
 :api-error-received
 [re-frame/trim-v]
 (fn [db [err]]
   (error err)
   (api/disconnect!)
   (dissoc db :downloads)))

(re-frame/register-handler
 :api-unknown-received
 [re-frame/trim-v]
 (fn [db [input]]
   (error input)
   (api/disconnect!)
   (dissoc db :downloads)))


(re-frame/register-handler
 :api-bad-msg-received
 [re-frame/trim-v]
 (fn [db [msg]]
   (error "Bad message received")
   (error (pr-str msg))
   db))

(re-frame/register-handler
 :api-notification-received
 [re-frame/trim-v]
 (fn [db [notification]]
   (api/on-notification-received notification)
   db))

(defn register-notification-handler
  [handler status]
  (re-frame/register-handler
   handler
   [re-frame/trim-v]
   (fn [db [gid]]
     (update-in db [:downloads gid] merge {:status status :gid gid}))))

(register-notification-handler :download-started      "active")
(register-notification-handler :download-paused       "paused")
(register-notification-handler :download-stopped      "paused")
(register-notification-handler :download-completed    "complete")
(register-notification-handler :download-errored      "error")
(register-notification-handler :bt-download-complete  "complete")

(re-frame/register-handler
 :filter-toggled
 [re-frame/trim-v]
 (fn [db [filt]]
   (let [active? (get-in db [:filters filt])]
     (assoc-in db [:filters filt] (not active?)))))

(re-frame/register-handler
 :new-download-form-show
 [re-frame/trim-v]
 (fn [db]
   (assoc db :new-download-form-showing true)))

(re-frame/register-handler
 :new-download-form-hide
 [re-frame/trim-v]
 (fn [db]
   (assoc db :new-download-form-showing false)))

(re-frame/register-handler
 :pause-download
 [re-frame/trim-v]
 (fn [db [dl]]
   (let [gid (:gid dl)]
     (when gid
       (re-frame/dispatch [:pause {:gid gid}])
       db))))

(re-frame/register-handler
 :resume-download
 [re-frame/trim-v]
 (fn [db [dl]]
   (let [gid (:gid dl)]
     (when gid
       (re-frame/dispatch [:unpause {:gid gid}])
       db))))

(re-frame/register-handler
 :api-connection-status-changed
 [re-frame/trim-v]
 (fn [db [new-status]]
   (assoc db :api-connection-status new-status)))


