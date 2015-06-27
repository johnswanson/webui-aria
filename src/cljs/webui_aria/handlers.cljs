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

(register-api-handler :add-uri)
(register-api-handler :get-status)

(defn handle-add-uri [db {id :id gid :result}]
  (update-in db [:downloads gid] assoc :status :initialized))

(defn handle-get-status [db {id :id status :result}]
  (update-in db [:downloads (:gid status)] merge status))

(defn handle-no-request [db response]
  (error "Response received with no request found")
  (error (clj->js response))
  db)

(defn handler [method]
  (let [handler ({:add-uri    handle-add-uri
                  :get-status handle-get-status
                  nil         handle-no-request} method)]
    (fn [db response]
      (-> db
          (handler response)
          (update-in [:pending-requests] dissoc (:id response))))))

(re-frame/register-handler
 :api-response-received
 [re-frame/debug re-frame/trim-v]
 (fn [db [{:keys [id result] :as response}]]
   (let [request (get-in db [:pending-requests id])]
     ((handler (:method request)) db response))))

(re-frame/register-handler
 :api-message-received
 [re-frame/debug re-frame/trim-v]
 (fn [db [message]]
   (api/on-message-received message)
   db))

(re-frame/register-handler
 :api-error-received
 [re-frame/debug re-frame/trim-v]
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
 :api-notification-received
 [re-frame/debug re-frame/trim-v]
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
(register-notification-handler :bt-download-completed :completed)

