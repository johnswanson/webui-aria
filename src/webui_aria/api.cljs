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
  (init [this])
  (start [this])
  (stop [this])
  (restart [this])
  (call! [this actions])
  (call-loop! [this])
  (call [this action])
  (params [this args])
  (action [this method params])
  (get-version [this])
  (start-download [this url])
  (get-status [this gid])
  (get-active [this])
  (get-stopped [this offset num])
  (get-waiting [this offset num])
  (pause-download [this gid])
  (unpause-download [this gid])
  (remove-download [this gid]))

(defn ws-url [{:keys [hostname port secure? path]}]
  (let [url (map->URL {:host hostname
                       :port port
                       :protocol (if secure? "wss" "ws")
                       :path path})]
    (str url)))

(defn parse [json]
  (utils/->kw-kebab (js->clj (.parse js/JSON json))))

(def ws-channel-transducer
  (map (fn [val]
         (let [{:keys [error message] :as input} (utils/->kw-kebab val)]
           (cond
             error    [:error-ch (parse error)]
             message  [:message-ch (parse message)]
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

(defn error-channel-transducer [api]
  (map (fn [err]
         [:action-ch (actions/from-error err)])))

(def send-channel-transducer
  (map (fn [val] [:ws-ch-write (.stringify js/JSON (clj->js val))])))

(defn log-flow [channels incoming-channel outgoing-channel-key value]
  (let [name-of-incoming-ch (fn [ch] (ffirst
                                      (filter (fn [[k v]]
                                                (= v ch))
                                              channels)))]
    (js/console.log (name (name-of-incoming-ch incoming-channel))
                    " => "
                    (name outgoing-channel-key)
                    "[ " (clj->js value) " ]")))

(defrecord Api [config channels responses action-ch state]
  IApi
  (init [this]
    (let [channels
          {:send-ch         (a/chan 1 send-channel-transducer)
           :ws-ch           (a/chan 1 ws-channel-transducer)
           :error-ch        (a/chan 1 (error-channel-transducer this))
           :message-ch      (a/chan 1 message-channel-transducer)
           :notification-ch (a/chan 1 notification-channel-transducer)}
          sinks {:ws-ch-write (a/chan)
                 :response-ch (a/chan)
                 :action-ch   (a/chan)}
          response-pub      (a/pub (:response-ch sinks) :id)]
      (go-loop []
        (let [[[channel-key value] incoming-ch] (a/alts! (vals channels))]
          (log-flow channels incoming-ch channel-key value)
          (if-let [ch (or (channels channel-key) (sinks channel-key))]
            (a/put! ch value)
            (a/put!
             (channels :error-ch)
             (str "invalid channel: "
                  (name channel-key)
                  ", from channel "
                  incoming-ch))))
        (recur))
      (assoc this
             :queue-ch (a/chan)
             :channels channels
             :sinks sinks
             :responses response-pub
             :state (atom {:status :initialized :ws-channel nil})
             :action-ch (sinks :action-ch))))
  (start [this]
    (call-loop! this)
    (when-not (= (:status @state) :connecting)
      (swap! state assoc :status :connecting)
      (go
        (let [url (ws-url config)
              read-ch (a/chan)
              write-ch (a/chan)
              ;; nil means: don't close my channel if ws-channel goes away!
              _ (a/pipe read-ch (:ws-ch (:channels this)) nil)
              _ (a/pipe (:ws-ch-write (:sinks this)) write-ch)
              {:keys [error]} (ws-ch url {:read-ch read-ch
                                          :write-ch write-ch})]
          (if-not error
            (swap! state assoc :ws-channel read-ch :status :connected)
            (swap! state assoc :ws-channel nil :status :error)))))
    this)
  (stop [this]
    (swap! state (fn [{:keys [status ws-channel] :as state}]
                   (case status
                     :connecting (do (go (a/<! (a/timeout 100)))
                                     state)
                     :connected (do (a/close! ws-channel)
                                    {:status :stopped})
                     :error (do (when ws-channel (a/close! ws-channel))
                                {:status :stopped})
                     :stopped (do (when ws-channel (a/close! ws-channel))
                                  {:status :stopped})))))
  (restart [this]
    (stop this)
    (start this))
  (call! [this actions]
    (when (seq actions)
      (let [response (a/chan)
            params (reduce (fn [ps a]
                             (conj ps {:methodName (:method a)
                                       :params     (:params a)}))
                           []
                           actions)
            multicall-action {:jsonrpc "2.0"
                              :id (uuid/uuid-string (uuid/make-random-uuid))
                              :method "system.multicall"
                              :params [params]}]
        (a/sub responses (:id multicall-action) response)
        (a/put! (channels :send-ch) multicall-action)
        (go (let [{:keys [result]} (a/<! response)
                  new-responses (map-indexed (fn [i [r]]
                                               {:id (:id (get actions i))
                                                :result r})
                                             result)]
              (doseq [response new-responses]
                (a/put! (get-in this [:sinks :response-ch]) response)))))))
  (call-loop! [this]
    (go-loop [as []]
      (let [timeout (a/timeout (-> config :queue :timeout))
            [a ch] (a/alts! [timeout (:queue-ch this)])
            timed-out? (= timeout ch)
            actions (if timed-out? as (conj as a))]
        (if (or timed-out?
                (> (count actions)
                   (-> config :queue :size)))
          (do
            (call! this actions)
            (recur []))
          (recur actions)))))
  (call [this action]
    (let [response (a/chan)]
      (a/sub responses (:id action) response)
      (a/put! (:queue-ch this) action)
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
            (actions/emit-version-received! action-ch resp)
            (a/close! ch)))))
  (start-download [this url]
    (let [act (action this "addUri" [[url] {"gid" (aria-gid)}])
          ch (call this act)]
      (go (let [{gid :result} (a/<! ch)]
            (actions/emit-download-init! action-ch gid)
            (a/close! ch)))))
  (get-status [this gid]
    (let [act (action this "tellStatus" [gid])
          ch (call this act)]
      (go (let [{status :result} (<! ch)]
            (actions/emit-status-received! action-ch gid status)
            (a/close! ch)))))
  (get-active [this]
    (let [act (action this "tellActive" [])
          ch (call this act)]
      (go (let [{statuses :result} (<! ch)]
            (doseq [status statuses]
              (actions/emit-status-received! action-ch (:gid status) status))
            (a/close! ch)))))
  (get-waiting [this offset num]
    (let [act (action this "tellWaiting" [offset num])
          ch (call this act)]
      (go (let [{statuses :result} (<! ch)]
            (doseq [status statuses]
              (actions/emit-status-received! action-ch (:gid status) status))
            (a/close! ch)))))
  (get-stopped [this offset num]
    (let [act (action this "tellStopped" [offset num])
          ch (call this act)]
      (go (let [{statuses :result} (<! ch)]
            (doseq [status statuses]
              (actions/emit-status-received! action-ch (:gid status) status))
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

(defonce api-atom (atom (init (map->Api {}))))

(defn api [config action-ch]
  (swap! api-atom assoc :config config)
  (let [a @api-atom]
    (a/pipe (:action-ch a) action-ch)
    (start a)))

