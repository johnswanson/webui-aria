(ns webui-aria.api
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame]
            [re-frame.utils :refer [log warn error]]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [put! chan close! timeout sliding-buffer]]
            [cljs-uuid-utils.core :as uuid]
            [cemerick.url :refer [map->URL]]
            [webui-aria.utils :as utils]))

(def base-call-data {:jsonrpc "2.0"})

(defn params
  "{:secret :hey :url :thing :foo :bar} , [:foo :url] => [:hey :bar :thing]
   {:secret :hey :foo :bar} , [:foo :url] => [:hey :bar]"
  [arg-obj arg-order]
  (let [kws (if (:secret arg-obj) (concat [:secret] arg-order) arg-order)
        ps (into [] (take-while identity (map #(% arg-obj) kws)))]
    (apply vector ps)))

(def method-info
  {:add-uri         ["aria2.addUri"        [:uris :options :position]]
   :get-status      ["aria2.tellStatus"    [:gid :keys]]
   :tell-active     ["aria2.tellActive"    [:keys]]
   :tell-waiting    ["aria2.tellWaiting"   [:offset :num]]
   :tell-stopped    ["aria2.tellStopped"   [:offset :num :keys]]
   :remove          ["aria2.remove"        [:gid]]
   :force-remove    ["aria2.forceRemove"   [:gid]]
   :unpause         ["aria2.unpause"       [:gid]]
   :unpause-all     ["aria2.unpauseAll"    []]
   :pause           ["aria2.pause"         [:gid]]
   :pause-all       ["aria2.pauseAll"      []]
   :force-pause     ["aria2.forcePause"    [:gid]]
   :force-pause-all ["aria2.forcePauseAll" []]
   :multicall       ["system.multicall"    [:methods]]})

(defn new-id [] (uuid/uuid-string (uuid/make-random-uuid)))

(defn request [method config args]
  (let [id         (new-id)
        as         (assoc args :id id :secret (when-not (= method :multicall)
                                                ;; treated specially
                                                (config :secret)))
        [method-str order] (method-info method)]
    (assoc base-call-data
           :id id
           :method method-str
           :params (params as order))))

(defn on-data-received [val]
  (let [{:keys [error message] :as input} (utils/->kw-kebab val)]
    (cond
      message (re-frame/dispatch [:api-message-received message])
      error   (re-frame/dispatch [:api-error-received   error])
      :else   (re-frame/dispatch [:api-unknown-received input]))))

(defn on-message-received [{:keys [id method params result] :as message}]
  (cond
    method (re-frame/dispatch [:api-notification-received {:method method
                                                           :params params}])
    id     (re-frame/dispatch [:api-response-received     {:id id
                                                           :result result}])
    (seq message)
    (doseq [msg message]
      (when msg (on-message-received msg)))

    :else  (re-frame/dispatch [:api-bad-msg-received      message])))

(defn on-notification-received [{method :method [{:keys [gid]}] :params :as notif}]
  (let [emission-type (get {"aria2.onDownloadStart"      :download-started
                            "aria2.onDownloadPause"      :download-paused
                            "aria2.onDownloadStop"       :download-stopped
                            "aria2.onDownloadComplete"   :download-completed
                            "aria2.onDownloadError"      :download-errored
                            "aria2.onBtDownloadComplete" :bt-download-complete}
                           method)]
    (if (and emission-type method gid)
      (re-frame/dispatch [emission-type gid])
      (re-frame/dispatch [:api-unknown-notification-received notif]))))

(defn url-from-config [{:keys [host port secure? path]}]
  (let [url (map->URL {:host host
                       :port port
                       :protocol (if secure? "wss" "ws")
                       :path path})]
    (str url)))

(defn receive-msgs! [ch]
  (go-loop []
    (when-let [rec (<! ch)]
      (on-data-received rec)
      (recur))))

(defn send-msgs! [<msgs >send]
  (go-loop [pending []]
    (let [t (timeout 100)
          [msg which-ch] (alts! [t <msgs])]
      (if (= which-ch t)
        (do
          (when (seq pending) (put! >send pending))
          (recur []))
        (when msg
          (recur (conj pending msg)))))))

(defonce state (atom {:status :disconnected}))

(add-watch state :status-change
           (fn [_ _ old new]
             (when (not= (:status old) (:status new))
               (re-frame/dispatch
                [:api-connection-status-changed (:status new)]))))

(defn connect! [desired-config]
  (let [conn (chan (sliding-buffer 10))]
    (swap! state assoc :config desired-config :conn conn :status :connecting)
    (go
      (let [{:keys [ws-channel error]}
            (<! (ws-ch (url-from-config desired-config)
                       {:format :json}))]
        (if error
          (swap! state assoc :status :disconnected)
          (do
            (receive-msgs! ws-channel)
            (send-msgs! conn ws-channel)
            (swap! state assoc :status :connected)))))
    conn))

(defn disconnect! []
  (when-let [conn (:conn @state)]  (close! conn))
  (swap! state assoc :status :disconnected))

(defn request! [desired-config request]
  (let [{:keys [config conn status]} @state]
    (if (= config desired-config)
      (case status
        :connected
        (put! conn request)

        :connecting (put! conn request)

        :disconnected
        (let [conn (connect! desired-config)]
          (put! conn request)))

      (let [conn (connect! desired-config)]
        (disconnect!)
        (put! conn request)))))

