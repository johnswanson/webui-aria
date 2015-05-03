(ns webui-aria.components.downloads
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent :refer [atom cursor]]
            [webui-aria.utils :as utils]
            [webui-aria.api :as api]
            [webui-aria.components.speed-chart :as speed-chart]
            [goog.format :as fmt]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn download [api urls]
  (doseq [url (str/split urls #"\n")]
    (api/start-download api url)))

(defn listen-for-timeout! [api downloads]
  (let [t 1000]
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

(defn controls [download-cursor api]
  (fn [download-cursor api]
    (let [{:keys [status gid]} @download-cursor
          display (case status
                    "active"   {:play nil        :pause :enabled  :stop :enabled}
                    "waiting"  {:play :disabled  :pause nil       :stop :disabled}
                    "paused"   {:play :enabled   :pause nil       :stop :enabled}
                    "error"    {:play :disabled  :pause nil       :stop :disabled}
                    "complete" {:play nil        :pause :disabled :stop :disabled}
                    "removed"  {:play :disabled  :pause nil       :stop :disabled})]
      [:div
       (when-let [pause-display (:pause display)]
         [:a.btn.pause-btn {:class (name pause-display)
                            :on-click #(api/pause-download api gid)}
          [:i.mdi-av-pause]])
       (when-let [play-display (:play display)]
         [:a.btn.play-btn {:class (name play-display)
                           :on-click #(api/unpause-download api gid)}
          [:i.mdi-av-play-arrow]])
       (when-let [stop-display (:stop display)]
         [:a.btn.stop-btn {:class (name stop-display)
                           :on-click #(api/remove-download api gid)}
          [:i.mdi-av-stop]])])))

(defn download-item [download-cursor api]
  (fn [download-cursor api]
    (let [{:keys [bittorrent
                  completed-length
                  total-length
                  download-speed
                  upload-speed
                  files] :as download} @download-cursor
          pct-finished (* 100 (/ completed-length total-length))]
      [:tr
       [:td {:rowSpan 2} [controls download-cursor api]]
       [:td {:rowSpan 2} (or (-> bittorrent :info :name) (-> files first :path))]
       [:td (fmt/numBytesToString download-speed 1) "/s"]
       [:td (fmt/fileSize completed-length 2) " / " (fmt/fileSize total-length 2)]
       [:td (fmt/numBytesToString upload-speed 1) "/s"]])))

(defn progress-bar [download-cursor]
  (fn [download-cursor]
    (let [{:keys [completed-length total-length]} @download-cursor
          pct-complete (* 100 (/ completed-length total-length))]
      [:tr
       [:td {:colSpan 5} [:div.progress {:style {:height "10px"}} [:div.determinate {:style {:width pct-complete}}]]]])))

(defn download-second-row [download-cursor api]
  (fn [download-cursor api]
    (let [{:keys [completed-length
                  total-length
                  upload-speed
                  download-speed
                  files] :as download}
          @download-cursor
          pct-finished (* 100 (/ completed-length total-length))]
      [:tr
       [:td {:colSpan 3} [speed-chart/speed-chart download-cursor]]])))

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
      [:div.new-download-form-container.section.container.row
       {:class (if-not (:viewing? @state) "hidden" "shown")}
       [:div
        [:form {:on-submit #(-> % (.preventDefault))}
         [:div.col.s12
          [:h5.center-align "URLs to download (one per line)"]
          [:div.input-field [:textarea.materialize-textarea.new-download-textarea
                             {:value (:urls @state)
                              :id id
                              :placeholder "urls"
                              :on-change #(swap! state assoc :urls (-> % .-target .-value))}]]]
         [:button.btn
          {:on-click #(do (swap! state (fn [{:keys [urls] :as state}]
                                         (download api urls)
                                         (assoc state :viewing? false :urls ""))))}
          [:i.mdi-file-file-download]
          "Start Download"]
         [:button.btn
          {:on-click #(swap! state assoc :viewing? false)}
          [:i.mdi-navigation-cancel]
          "Cancel"]]]])))

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
         [:table
          [:thead [:tr
                   [:th {:data-field "controls"} "Controls"]
                   [:th {:data-field "file"} "File"]
                   [:th {:data-field "download-speed"} "Download Speed"]
                   [:th {:data-field "download-pct"} "Download Size"]
                   [:th {:data-field "upload"} "Upload"]]]
          [:tbody
           (interleave
            (for [gid (keys filtered)]
              ^{:key (str gid "-bar")} [progress-bar (cursor downloads [gid])])
            (for [gid (keys filtered)]
              ^{:key (str gid "-row1")} [download-item (cursor downloads [gid]) api])
            (for [gid (keys filtered)]
              ^{:key (str gid "-row2")} [download-second-row (cursor downloads [gid]) api]))]]]))))

