(ns webui-aria.components.downloads
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent :refer [atom cursor]]
            [goog.format :as fmt]
            [webui-aria.utils :as utils]
            [webui-aria.api :as api]
            [webui-aria.components.speed-chart :as speed-chart])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def download-actions [:download-init
                       :download-started
                       :download-paused
                       :download-error
                       :download-stopped
                       :download-complete
                       :bt-download-complete
                       :status-received])

(defn download-action-ch [pub]
  (let [ch (a/chan)]
    (apply utils/sub-multiple pub ch download-actions)
    ch))

(defn listen-for-timeout! [api downloads]
  (let [t 1000]
    (go-loop [ch (a/timeout t)]
      (let [_ (a/<! ch)]
        (api/get-active api)
        (api/get-waiting api 0 10000)
        (api/get-stopped api 0 10000)
        (doseq [gid (keys @downloads)]
          (api/get-status api gid))
        (recur (a/timeout t))))))

(defn listen-for-notifications!
  "Listens to actions, and updates downloads accordingly."
  [pub downloads]
  (let [ch (download-action-ch pub)]
    (go-loop []
      (let [[action-type {:keys [gid] :as dl}] (a/<! ch)
            new-dl (case action-type
                     :download-init        (assoc dl :status "waiting")
                     :download-started     (assoc dl :status "active")
                     :download-paused      (assoc dl :status "paused")
                     :download-error       (assoc dl :status "error")
                     :download-stopped     (assoc dl :status "complete")
                     :download-complete    (assoc dl :status "complete")
                     :bt-download-complete (assoc dl :status "complete")
                     :status-received      dl
                     (throw (js/Error "Unhandled action-type: " action-type)))
            update-download (fn [dl] (merge dl new-dl))]
        (swap! downloads update-in [gid] update-download)
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
                    "removed"  {:play :disabled  :pause nil       :stop :disabled}
                    "linked"   {:play :disabled  :pause nil       :stop :disabled})]
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

(defn download-speed-component [cursor]
  (let [{:keys [download-speed completed-length total-length]} @cursor]
    [:div
     [:i.mdi-file-file-download] (fmt/numBytesToString download-speed 1)
     "/s (" (fmt/fileSize completed-length 2) " / " (fmt/fileSize total-length 2) ")"]))

(defn upload-speed-component [cursor]
  (let [{:keys [upload-speed upload-length total-length]} @cursor]
    [:div
     [:i.mdi-file-file-upload] (fmt/numBytesToString upload-speed 1)
     "/s (" (fmt/fileSize upload-length 2) " / " (fmt/fileSize total-length 2) ")"]))

(defn download-item [download-cursor api]
  (fn [download-cursor api]
    (let [{:keys [bittorrent files] :as download} @download-cursor]
      [:div.row.valign-wrapper.download-item
       [:div.col.s3.valign [controls download-cursor api]]
       [:div.col.s3.valign (or (-> bittorrent :info :name) (-> files first :path))]
       [:div.col.s6
        [:div.row
         [:div.col.s10.offset-s2 [download-speed-component download-cursor]]]
        [:div.row
         [:div.col.s10.offset-s2 [upload-speed-component download-cursor]]]
        [:div.row.container
         [:div.col.s12 [speed-chart/speed-chart download-cursor]]]]])))

(defn download-container [even? download-cursor api]
  (fn [even? download-cursor api]
    (let [{:keys [completed-length total-length]} @download-cursor
          pct-complete (if (not= total-length 0) (* 100 (/ completed-length total-length)) 0)]
      [:div.download.progress.download-progress {:class (if even? "even" "odd")}
       [:div.determinate.download-completed-progress {:style {:width (str pct-complete "%")}}]
       [download-item download-cursor api]])))

(defn filter-set [filters-map]
  (into #{} (map key (filter val filters-map))))

(defn downloads-component [filters api pub]
  (let [downloads (atom (array-map))]
    (listen-for-notifications! pub downloads)
    (listen-for-timeout! api downloads)
    (fn [filters api pub]
      (let [s (filter-set @filters)
            display? #(s (keyword (:status (val %))))
            filtered (into {} (filter display? @downloads))]
        [:div
         [:h3 "Downloads"]
         [:div.downloads
          (map (fn [[gid _] even?]
                 ^{:key gid} [download-container even? (cursor downloads [gid]) api])
               filtered
               (interleave (repeat true) (repeat false)))]]))))
