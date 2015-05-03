(ns webui-aria.components.downloads
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent :refer [atom cursor]]
            [webui-aria.utils :as utils]
            [webui-aria.api :as api]
            [webui-aria.components.speed-chart :as speed-chart]
            [goog.format :as fmt]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn download [api state]
  (doseq [url (str/split (:urls @state) #"\n")]
    (api/start-download api url)))

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

(defn download-item [download-cursor]
  (fn [download-cursor]
    (let [{:keys [bittorrent
                  completed-length
                  total-length
                  download-speed
                  upload-speed] :as download} @download-cursor
          pct-finished (* 100 (/ completed-length total-length))]
      [:div
       [:div.row
        [:div.col.s10.offset-s1
         [:div.progress
          [:div.determinate {:style {:width (str pct-finished "%")}}]]]]
       [:div.row
        [:div.col.s4.offset-s1 (or (-> bittorrent :info :name) "unknown")]
        [:div.col.s1
         [:i.mdi-file-file-download.tiny]
         [:span.download-speed (fmt/numBytesToString download-speed 0) "/s"]]
        [:div.col.s1
         [:span.download-amount (fmt/fileSize completed-length) " / " (fmt/fileSize total-length)]]
        [:div.col.s1.download [speed-chart/speed-chart download-cursor :download-speed]]
        [:div.col.s1.offset-s1
         [:i.mdi-file-file-upload.tiny]
         [:span (fmt/numBytesToString upload-speed) "/s"]]
        [:div.col.s1.upload [speed-chart/speed-chart download-cursor :upload-speed]]]])))

(defn listen-for-adding-new-download! [pub state]
  (let [ch (a/chan)]
    (a/sub pub :begin-viewing-download-form ch)
    (go-loop []
      (let [_ (a/<! ch)]
        (swap! state assoc :viewing? true)
        (recur)))))

(defn new-download [api pub]
  (let [state (atom nil)
        id (utils/rand-hex-str 8)]
    (listen-for-adding-new-download! pub state)
    (fn [api pub]
      (when (:viewing? @state)
        [:div
         [:div.row
          [:div.col.s12
           [:div.row
            [:div.input-field.col.s12
             [:textarea.materialize-textarea {:value (:urls @state)
                                              :id id
                                              :placeholder "urls"
                                              :on-change #(swap! state assoc :urls (-> % .-target .-value))}
              (:urls @state)]]
            [:div.col.s6]]]]
         [:div.row
          [:div.col.12
           [:a.btn
            {:on-click #(do (swap! state assoc :viewing? false)
                            (download api state))}
            "Start Download"]]]]))))

(defn filter-set [filters-map]
  (into #{} (map key (filter val filters-map))))

(defn downloads-component [filters api pub]
  (let [downloads (atom (array-map))]
    (listen-for-notifications! pub downloads)
    (listen-for-timeout! api downloads)
    (api/get-active api)
    (api/get-waiting api)
    (api/get-stopped api)
    (fn [filters api pub]
      (let [s (filter-set @filters)
            display? #(s (keyword (:status (val %))))
            filtered (into {} (filter display? @downloads))]
        [:div
         [new-download api pub]
         [:h3.center-align "Downloads"]
         (for [gid (keys filtered)]
           ^{:key gid} [download-item (cursor downloads [gid])])]))))
