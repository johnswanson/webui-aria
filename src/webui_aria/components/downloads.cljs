(ns webui-aria.components.downloads
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent :refer [atom cursor]]
            [webui-aria.api :as api]
            [webui-aria.components.speed-chart :as speed-chart])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn download [api state]
  (api/start-download api (:url @state)))

(defn listen-for-timeout! [api downloads]
  (let [t 1500]
    (go-loop [ch (a/timeout t)]
      (let [_ (<! ch)]
        (doseq [gid (keys @downloads)]
          (api/get-status api gid))
        (recur (a/timeout t))))))

(defn listen-for-notifications! [pub downloads]
  (let [ch (a/chan)]
    (a/sub pub :download-init ch)
    (a/sub pub :download-started ch)
    (a/sub pub :download-paused ch)
    (a/sub pub :download-stopped ch)
    (a/sub pub :download-complete ch)
    (a/sub pub :download-error ch)
    (a/sub pub :bt-download-complete ch)
    (a/sub pub :status-received ch)
    (go-loop []
      (let [[action-type {:keys [gid] :as download}] (<! ch)
            download-state (case action-type
                             :download-init "waiting"
                             :download-started "active"
                             :download-paused "paused"
                             :download-error "error"
                             :download-stopped "complete"
                             :download-complete "complete"
                             :bt-download-complete "complete"
                             :status-received (:status download)
                             nil)]
        (swap! downloads update-in [gid] #(merge % (if download-state
                                                 (assoc download :status download-state)
                                                 download)))
        (recur)))))

(defn download-item []
  (fn [download-cursor]
    (let [{:keys [status gid] :as download} @download-cursor]
      [:li
       [:div
        [:div.state status]
        [:div (str download)]
        [speed-chart/speed-chart download-cursor]
        [:div.gid gid]]])))

(defn new-download [api]
  (let [state (atom nil)]
    (fn []
      [:div
       [:input {:value (:url @state)
                :on-change #(swap! state assoc :url (-> % .-target .-value))}
        (:url @state)]
       [:button {:on-click #(download api state)} "Start Download"]])))

(defn downloads-component [api pub]
  (let [downloads (atom (array-map))]
    (listen-for-notifications! pub downloads)
    (listen-for-timeout! api downloads)
    (api/get-active api)
    (api/get-waiting api)
    (api/get-stopped api)
    (fn []
      [:div
       [:div [:p "Downloads"]
        [:ul
         (for [gid (keys @downloads)]
           ^{:key gid} [download-item (cursor downloads [gid])])]]
       [new-download api]])))
