(ns webui-aria.api
  (:require [cljs.core.async :as a]
            [chord.client :refer [ws-ch]]
            [cljs-uuid-utils.core :as uuid]
            [webui-aria.actions :refer [emit-version-received! emit-download-started!]])
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

(defn api-response-channels []
  (let [recv (a/chan)
        send (a/chan)]
    (go
      (let [{:keys [ws-channel error]}
            (a/<! (ws-ch "ws://localhost:6800/jsonrpc" {:format :json}))]
        (if-not error
          (do (receive-messages! recv ws-channel)
              (send-messages! send ws-channel))
          (print "Could not connect to server!"))))
    [recv send]))

(defn make-actions [action-ch recv]
  (go-loop []
    (let [msg (a/<! recv)]
      (case msg
        (print msg))
      (recur))))

(defn error? [server-response] (:error server-response))
(defn message [server-response] (:message server-response))
(defn error [server-response] (:error server-response))
(defn split-off-errors [ch]
  (let [[errs msgs] (a/split :error ch)
        err-channel (a/chan 1 (map error))
        msg-channel (a/chan 1 (map message))]
    (a/pipe errs err-channel)
    (a/pipe msgs msg-channel)
    [err-channel msg-channel]))
(defn p [api] (:pub api))

(defn id [msg]
  (msg "id"))

(defn new-id []
  (uuid/uuid-string (uuid/make-random-uuid)))

(defprotocol IApi
  (init [this])
  (call [this action])
  (action [this method params])
  (get-version [this])
  (start-download [this url]))

(defrecord Api [config action-chan]
  IApi
  (init [this]
    (let [[recv send] (api-response-channels)
          [errs msgs] (split-off-errors recv)]
      (assoc this
             :send-channel send
             :pub (a/pub msgs id))))
  (call [this action]
    (let [subbed (a/chan)]
      (a/sub (:pub this) (:id action) subbed)
      (a/put! (:send-channel this) action)
      subbed))
  (action [this method params]
    {:jsonrpc "2.0"
     :id (new-id)
     :method (str "aria2." method)
     :params (if-let [token (:token config)]
               (vec (concat [(str "token:" token)] params))
               params)})
  (get-version [this]
    (let [act (action this "getVersion" [])
          ch (call this act)]
      (go (let [resp (a/<! ch)]
            (a/unsub (:pub this) (:id act) ch)
            (emit-version-received! action-chan resp)))))
  (start-download [this url]
    (print (action this "addUri" [[url]]))
    (let [act (action this "addUri" [[url]])
          ch (call this act)]
      (go (let [resp (a/<! ch)]
            (a/unsub (:pub this) (:id act) ch)
            (emit-download-started! action-chan resp))))))

(defn make-api [config action-chan]
  (init
   (map->Api {:config config :action-chan action-chan})))

