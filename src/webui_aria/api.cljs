(ns webui-aria.api
  (:require [cljs.core.async :as a]
            [cljs-uuid-utils.core :as uuid]
            [chord.client :refer [ws-ch]]
            [cemerick.url :refer [map->URL]]
            [webui-aria.actions :as actions]
            [webui-aria.utils :refer
             [aria-endpoint aria-gid hostname]
             :as utils]
            [webui-aria.api.response :as response])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defprotocol IApi
  (init [this action-ch])
  (start [this])
  (stop [this])
  (call [this action])
  (params [this args])
  (action [this method params])
  (get-version [this])
  (start-download [this url])
  (get-status [this gid])
  (get-active [this])
  (get-stopped [this])
  (get-waiting [this])
  (pause-download [this gid])
  (unpause-download [this gid])
  (remove-download [this gid]))

(defn ws-url [{:keys [hostname port secure? path]}]
  (let [url (map->URL {:host hostname
                       :port port
                       :protocol (if secure? "wss" "ws")
                       :path path})]
    (str url)))

(def ws-channel-transducer
  (map (fn [val]
         (let [{:keys [error message] :as input} (utils/->kw-kebab val)]
           (cond
             error    [:error-ch error]
             message  [:message-ch message]
             :else    [:error-ch input])))))

(def message-channel-transducer
  (map (fn [{:keys [id method params result] :as message}]
         (cond
           id     [:response-ch     {:id id :result result}]
           method [:notification-ch {:method method :params params}]
           :else  [:error-ch message]))))

(def notification-channel-transducer
  (map (fn [{method :method [{:keys [gid]}] :params :as notification}]
         (let [emission (case method
                          "aria2.onDownloadStart" :download-started
                          "aria2.onDownloadPause" :download-paused
                          "aria2.onDownloadStop" :download-stopped
                          "aria2.onDownloadComplete" :download-completed
                          "aria2.onDownloadError" :download-errored
                          "aria2.onBtDownloadComplete" :bt-download-completed
                          nil)]
           (if (and emission method gid)
             [:action-ch (actions/from-notification emission gid)]
             [:error-ch notification])))))

(def error-channel-transducer
  (map (fn [err]
         [:action-ch (actions/from-error err)])))

(def send-channel-transducer
  (map (fn [val] [:ws-ch val])))

(defn log-flow [channels incoming-channel outgoing-channel-key value]
  (let [name-of-incoming-ch (fn [ch] (ffirst
                                      (filter (fn [[k v]]
                                                (= v ch))
                                              channels)))]
    (js/console.log (name (name-of-incoming-ch incoming-channel))
                    " => "
                    (name outgoing-channel-key)
                    "[ " (clj->js value) " ]")))

(defrecord Api [config channels ws-channel-atom responses action-ch]
  IApi
  (init [this action-ch]
    (let [channels
          {:send-ch         (a/chan 1 send-channel-transducer)
           :ws-ch           (a/chan 1 ws-channel-transducer)
           :error-ch        (a/chan 1 error-channel-transducer)
           :message-ch      (a/chan 1 message-channel-transducer)
           :response-ch     (a/chan)
           :notification-ch (a/chan 1 notification-channel-transducer)}
          response-pub      (a/pub (:response-ch channels) :id)]
      (go-loop []
        (let [[[channel-key value] incoming-ch] (a/alts! (vals channels))]
          (log-flow channels incoming-ch channel-key value)
          (cond
            (channels channel-key)     (a/put! (channels channel-key) value)
            (= channel-key :action-ch) (a/put! action-ch value)
            :else                      (a/put!
                                        (channels :error-ch)
                                        (str "invalid channel: "
                                             (name channel-key)
                                             ", from channel "
                                             incoming-ch))))
        (recur))
      (assoc this
             :channels channels
             :responses response-pub
             :ws-channel-atom (atom nil)
             :action-ch action-ch)))
  (start [this]
    (when-not @ws-channel-atom
      (go
        (let [url (ws-url config)
              {:keys [ws-channel error]} (ws-ch url {:format :json})]
          (if ws-channel
            (swap! ws-channel-atom (fn [_]
                                     (a/pipe ws-channel (channels :ws-ch) nil)
                                     ws-channel))
            (actions/emit-connection-failed!
             action-ch
             error)))))
    this)
  (stop [this]
    (swap! ws-channel-atom (fn [ws-channel]
                             (when ws-channel
                               (a/close! ws-channel))
                             nil)))
  (call [this action]
    (let [response (a/chan)]
      (a/sub responses (:id action) response)
      (a/put! (-> channels :send-ch) action)
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
            (actions/emit-version-received! (channels :action-ch) resp)
            (a/close! ch)))))
  (start-download [this url]
    (let [act (action this "addUri" [[url] {"gid" (aria-gid)}])
          ch (call this act)]
      (go (let [{gid :result} (a/<! ch)]
            (actions/emit-download-init! (channels :action-ch) gid)
            (a/close! ch)))))
  (get-status [this gid]
    (let [act (action this "tellStatus" [gid])
          ch (call this act)]
      (go (let [{status :result} (<! ch)]
            (actions/emit-status-received! (channels :action-ch) gid status)
            (a/close! ch)))))
  (get-active [this]
    (let [act (action this "tellActive" [])
          ch (call this act)]
      (go (let [{statuses :result} (<! ch)]
            (doseq [status statuses]
              (actions/emit-status-received! (channels :action-ch) (:gid status) status))
            (a/close! ch)))))
  (get-waiting [this]
    (let [act (action this "tellWaiting" [])
          ch (call this act)]
      (go (let [{statuses :result} (<! ch)]
            (doseq [status statuses]
              (actions/emit-status-received! (channels :action-ch) (:gid status) status))
            (a/close! ch)))))
  (get-stopped [this]
    (let [act (action this "tellStopped" [])
          ch (call this act)]
      (go (let [{statuses :result} (<! ch)]
            (doseq [status statuses]
              (actions/emit-status-received! (channels :action-ch) (:gid status) status))
            (a/close! ch)))))
  (pause-download [this gid]
    (let [act (action this "pause" [gid])]
      (call this act)))
  (unpause-download [this gid]
    (let [act (action this "unpause" [gid])]
      (call this act)))
  (remove-download [this gid]
    (let [act (action this "remove" [gid])]
      (call this act))))

(defn api [config action-ch]
  (start (init (map->Api {:config config}) action-ch)))

