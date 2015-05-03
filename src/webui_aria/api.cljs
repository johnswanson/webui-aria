(ns webui-aria.api
  (:require [cljs.core.async :as a]
            [cljs-uuid-utils.core :as uuid]
            [chord.client :refer [ws-ch]]
            [webui-aria.actions :as actions]
            [webui-aria.utils :refer [aria-endpoint aria-gid hostname] :as utils]
            [webui-aria.api.response :as response])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;;; The API receives calls, like (start-download api
;;;                               {:url "http://example.com"})
;;; The API listens for responses on the websocket, and creates actions from
;;; them.

(defn receive-messages! [out server-ch]
  (go-loop []
    (when-let [msg (a/<! server-ch)]
      (a/>! out msg)
      (recur))))

(defn send-messages! [send-ch server-ch]
  (go-loop []
    (when-let [msg (a/<! send-ch)]
      (a/>! server-ch msg)
      (recur))))

(defn message [server-response] (:message server-response))
(defn error [server-response] (:error server-response))

(defn connect [url]
  (let [recv (a/chan)
        send (a/chan)
        [errs msgs] (a/split :error recv)
        error-channel (a/chan 1 (map error))
        _ (a/pipe errs error-channel)
        message-channel (a/chan 1 (map message))
        _ (a/pipe msgs message-channel)
        [responses notifications] (a/split response/response? message-channel)]
    (go
      (let [{:keys [ws-channel error]}
            (a/<! (ws-ch url {:format :json}))]
        (if-not error
          (do (receive-messages! recv ws-channel)
              (send-messages! send ws-channel))
          (throw (js/Error "Could not connect to server at " url)))))
    {:error-channel error-channel
     :responses (a/pub responses response/id)
     :notifications notifications
     :send-channel send}))

(defprotocol IApi
  (start [this])
  (call [this action])
  (params [this args])
  (action [this method params])
  (get-version [this])
  (start-download [this url])
  (get-status [this gid])
  (get-active [this])
  (get-stopped [this])
  (get-waiting [this]))

(defn listen-for-notifications! [notifications action-chan]
  (go-loop []
    (let [{method "method" [{gid "gid"}] "params"} (a/<! notifications)
          emission (case method
                     "aria2.onDownloadStart" actions/emit-download-started!
                     "aria2.onDownloadPause" actions/emit-download-paused!
                     "aria2.onDownloadStop" actions/emit-download-stopped!
                     "aria2.onDownloadComplete" actions/emit-download-complete!
                     "aria2.onDownloadError" actions/emit-download-error!
                     "aria2.onBtDownloadComplete" actions/emit-bt-download-complete!
                     #(throw (js/Error (str "Unknown method " method ", gid " %2))))]
      (emission action-chan gid)
      (recur))))

(defrecord Api [config action-chan responses notifications error-ch send-ch]
  IApi
  (start [this]
    (let [{:keys [error-channel
                  responses
                  notifications
                  send-channel]} (connect (str "ws://" hostname ":6800/jsonrpc"))]
      (listen-for-notifications! notifications action-chan)
      (assoc this
             :send-ch send-channel
             :responses responses
             :notifications notifications
             :error-ch error-channel)))
  (call [this action]
    (let [response (a/chan 1 (map utils/->kw-kebab))]
      (a/sub responses (:id action) response)
      (a/put! send-ch action)
      response))
  (params [this args]
    (if-let [token (:token config)]
      (vec (concat [(str "token:" token)] args))
      args))
  (action [this method args]
    {:jsonrpc "2.0"
     :id (uuid/uuid-string (uuid/make-random-uuid))
     :method (str "aria2." method)
     :params (params this args)})
  (get-version [this]
    (let [act (action this "getVersion" [])
          ch (call this act)]
      (go (let [resp (a/<! ch)]
            (actions/emit-version-received! action-chan resp)))))
  (start-download [this url]
    (let [act (action this "addUri" [[url] {"gid" (aria-gid)}])
          ch (call this act)]
      (go (let [{gid :result} (a/<! ch)]
            (actions/emit-download-init! action-chan gid)
            (a/close! ch)))))
  (get-status [this gid]
    (let [act (action this "tellStatus" [gid])
          ch (call this act)]
      (go (let [{status :result} (<! ch)]
            (actions/emit-status-received! action-chan gid status)
            (a/close! ch)))))
  (get-active [this]
    (let [act (action this "tellActive" [])
          ch (call this act)]
      (go (let [{statuses :result} (<! ch)]
            (doseq [status statuses]
              (actions/emit-status-received! action-chan (:gid status) status))
            (a/close! ch)))))
  (get-waiting [this]
    (let [act (action this "tellWaiting" [])
          ch (call this act)]
      (go (let [{statuses :result} (<! ch)]
            (doseq [status statuses]
              (actions/emit-status-received! action-chan (:gid status) status))
            (a/close! ch)))))
  (get-stopped [this]
    (let [act (action this "tellStopped" [])
          ch (call this act)]
      (go (let [{statuses :result} (<! ch)]
            (doseq [status statuses]
              (actions/emit-status-received! action-chan (:gid status) status))
            (a/close! ch))))))

(defn make-api [config action-chan]
  (start
   (map->Api {:config config :action-chan action-chan})))

