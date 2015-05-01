(ns webui-aria.components.downloads
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent :refer [atom cursor]]
            [webui-aria.utils :as utils]
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
    (let [{:keys [status gid bittorrent] :as download} @download-cursor]
      [:div.row
       [:div.col.s3 (or (-> bittorrent :info :name) "unknown")]
       [:div.col.s1 status]
       [:div.col.s4
        [:div.row
         [:div.col.s2.valign-wrapper
          [:i.mdi-file-file-upload.medium.valign]]
         [:div.col.s1 (:download-speed download)]
         [:div.col.s9.download [speed-chart/speed-chart download-cursor :download-speed]]]]
       [:div.col.s4
        [:div.row
         [:div.col.s2.valign-wrapper
          [:i.mdi-file-file-upload.medium.valign]]
         [:div.col.s1 (:upload-speed download)]
         [:div.col.s9.upload [speed-chart/speed-chart download-cursor :upload-speed]]]]])))

(defn new-download [api]
  (let [state (atom nil)
        id (utils/rand-hex-str 8)]
    (fn []
      [:div
       [:div.row
        [:div.col.s12
         [:div.row
          [:div.input-field.col.s6
           [:input {:value (:url @state)
                    :id id
                    :placeholder "url"
                    :on-change #(swap! state assoc :url (-> % .-target .-value))}
            (:url @state)]]
          [:div.col.s6]]]]
       [:div.row
        [:div.col.12
         [:a.waves-effect.waves-light.btn
          {:on-click #(download api state)}
          "Start Download"]]]])))

(defn downloads-component [api pub]
  (let [downloads (atom (array-map))]
    (listen-for-notifications! pub downloads)
    (listen-for-timeout! api downloads)
    (api/get-active api)
    (api/get-waiting api)
    (api/get-stopped api)
    (fn []
      [:div
       (for [gid (keys @downloads)]
         ^{:key gid} [download-item (cursor downloads [gid])])
       [new-download api]])))
