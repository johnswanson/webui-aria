(ns webui-aria.api
  (:require [cljs.core.async :as a]
            [cljs-uuid-utils.core :as uuid]
            [chord.client :refer [ws-ch]]
            [webui-aria.actions :refer [emit-version-received! emit-download-init!]]
            [webui-aria.utils :refer [aria-endpoint aria-gid hostname]]
            [webui-aria.api.notification :as notification]
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
     :notifications (a/pub notifications notification/gid)
     :send-channel send}))

(defprotocol IApi
  (start [this])
  (call [this action])
  (params [this args])
  (action [this method params])
  (get-version [this])
  (start-download [this url]))

(defn emission [action-chan type]
  (fn [data] (({:get-version #(emit-version-received! action-chan data)
                :init-download #(emit-download-init! action-chan data)} type) data)))

(defn emit [api type ch & [data]]
  (go (let [resp (a/<! ch)
            f (emission (:action-chan api) type)]
        (f (merge resp data))
        (a/close! ch))))

(defrecord Api [config action-chan responses notifications error-ch send-ch]
  IApi
  (start [this]
    (let [{:keys [error-channel
                  responses
                  notifications
                  send-channel]} (connect (str "ws://" hostname ":6800/jsonrpc"))]
      (assoc this
             :send-ch send-channel
             :responses responses
             :notifications notifications
             :error-ch error-channel)))
  (call [this action]
    (let [response (a/chan)]
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
      (emit this :get-version ch)))
  (start-download [this url]
    (let [act (action this "addUri" [[url] {"gid" (aria-gid)}])
          ch (call this act)]
      (emit this :init-download ch {:url url}))))

(defn make-api [config action-chan]
  (start
   (map->Api {:config config :action-chan action-chan})))

