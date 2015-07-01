(ns webui-aria.handlers
  (:require [re-frame.core :as re-frame]
            [re-frame.utils :refer [error warn log]]
            [cljs-uuid-utils.core :as uuid]
            [webui-aria.db :as db]
            [webui-aria.api :as api]))

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
  (reduce (fn [db status]
            (update-in db [:downloads (:gid status)] merge status))
          db
          statuses))

(def api-handlers
  {:add-uri
   (fn [db {gid :result} _]
     (update-in db [:downloads gid] assoc :status :initialized))
   :get-status
   (fn [db {status :result} _]
     (let [actual-download-status (if (:followed-by status)
                                    "linked"
                                    (:status status))]
       (update-in db [:downloads (:gid status)]
                  merge
                  status
                  {:status actual-download-status})))
   :tell-active
   (fn [db {[& statuses] :result} _]
     (apply-status-updates db statuses))
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
 (fn [db [error ch]]
   (error (clj->js error))
   (api/disconnect! ch)
   db))

(re-frame/register-handler
 :api-unknown-received
 [re-frame/trim-v]
 (fn [db [input]]
   (error (clj->js input))
   db))

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
     (assoc-in db [:downloads gid :status] status))))

(register-notification-handler :download-started      :started)
(register-notification-handler :download-paused       :paused)
(register-notification-handler :download-stopped      :stopped)
(register-notification-handler :download-completed    :completed)
(register-notification-handler :download-errored      :errored)
(register-notification-handler :bt-download-complete  :completed)

(re-frame/register-handler
 :filter-toggled
 [re-frame/trim-v]
 (fn [db [filt]]
   (let [active? (get-in db [:filters filt])]
     (assoc-in db [:filters filt] (not active?)))))

