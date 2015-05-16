(ns webui-aria.components.downloads
  (:require [reagent.core :as reagent :refer [atom cursor]]
            [re-frame.core :refer [subscribe dispatch]]
            [goog.format :as fmt])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn speed [key dl]
  (fn [key dl]
    (let [speed (get dl key)]
      [:span.speed (fmt/numBytesToString speed 1) "/s"])))

(defn completed-length [{:keys [completed-length]}]
  (fn []
    [:span.download-completed-length (fmt/fileSize completed-length 2)]))

(defn upload-length [{:keys [upload-length]}]
  (fn []
    [:span.download-upload-length (fmt/fileSize upload-length 2)]))

(defn total-length [{:keys [total-length]}]
  (fn []
    [:span.download-total-length (fmt/fileSize total-length 2)]))

(defn download-speed-box [dl]
  [:div.download-speed-box
   [speed :download-speed dl] [completed-length dl] " / " [total-length dl]])

(defn upload-speed-box [dl]
  [:div.upload-speed-box
   [speed :upload-speed dl] [upload-length dl] " / " [total-length dl]])

(defn download-name [dl]
  [:div.download-name (or (-> dl :bittorrent :info :name)
                          (-> dl :files first :path))])

(defn play-button [display? {:keys [gid]}]
  (when display?
    [:a.btn.play-btn {:on-click #(dispatch [:play-download gid])
                      :class    (name display?)}]))

(defn pause-button [display? {:keys [gid]}]
  (when display?
    [:a.btn.pause-btn {:on-click #(dispatch [:pause-download gid])
                       :class    (name display?)}]))

(defn stop-button [display? {:keys [gid]}]
  (when display?
    [:a.btn.stop-btn {:on-click #(dispatch [:stop-download gid])
                       :class    (name display?)}]))

(defn controls [{:keys [status] :as download}]
  (fn []
    (let [disp? (case status
                 "active"   {:play nil        :pause :enabled  :stop :enabled}
                 "waiting"  {:play :disabled  :pause nil       :stop :disabled}
                 "paused"   {:play :enabled   :pause nil       :stop :enabled}
                 "error"    {:play :disabled  :pause nil       :stop :disabled}
                 "complete" {:play nil        :pause :disabled :stop :disabled}
                 "removed"  {:play :disabled  :pause nil       :stop :disabled}
                 "linked"   {:play :disabled  :pause nil       :stop :disabled})]
      [:div
       [pause-button (:pause disp?) download]
       [play-button  (:play disp?)  download]
       [stop-button  (:stop disp?)  download]])))

(defn download-item [download]
  (fn [download]
    [:div.download-item
     [controls download]
     [download-name download]
     [download-speed-box download]
     [upload-speed-box download]]))

(defn download-items [downloads]
  (fn [downloads]
    [:div
     [:h3 "Downloads"]
     [:div.downloads
      (map (fn [download]
             ^{:key (:gid download)} [download-item download])
           downloads)]]))
