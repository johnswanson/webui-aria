(ns webui-aria.components.download
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [re-frame.utils :refer [log warn error]]
            [re-com.core :refer [v-box h-box md-icon-button]]
            [reagent.core :as reagent]
            [goog.format :as fmt]))

(defn download-speed [dl]
  (fn [dl]
    (let [speed     (-> dl
                        :download-speed
                        (fmt/numBytesToString 1))
          size      (-> dl
                        :total-length
                        (fmt/fileSize 1))
          completed (-> dl
                        :completed-length
                        (fmt/fileSize 1))]
      [:span speed "/s down (" completed " / " size " complete)"])))

(defn upload-speed [dl]
  (let [speed (-> dl
                  :upload-speed
                  (fmt/numBytesToString 1))]
    [:span speed "/s up"]))

(defn progress [dl]
  (fn [dl]
    (let [pct (* 100 (/ (:completed-length dl) (:total-length dl)))]
      [:div {:style {:position "absolute"
                     :top "0px"
                     :left "0px"
                     :z-index "-1"
                     :height "80%"
                     :background-color "#DDD"
                     :width (str pct "%")}}])))

(defn filename [dl]
  (fn [dl]
    [:span (or (get-in dl [:bittorrent :info :name])
                (-> dl :files first :path))]))

(defn speeds [dl]
  [h-box
   :children [[download-speed dl] [upload-speed dl]]
   :gap "2em"])

(defn play-button [disabled? dl]
  [md-icon-button
   :md-icon-name "md-play-arrow"
   :disabled? disabled?
   :on-click #(dispatch [:resume-download dl])])

(defn pause-button [disabled? dl]
  [md-icon-button
   :md-icon-name "md-pause"
   :disabled? disabled?
   :on-click #(dispatch [:pause-download dl])])

(defn stop-button [disabled? dl]
  [md-icon-button
   :md-icon-name "md-stop"
   :disabled? disabled?
   :on-click #(dispatch [:stop-download dl])])

(defn controls [{:keys [status] :as download}]
  (fn [{:keys [status] :as download}]
    (let [dis? (case status
                 "active"   {:play :disabled :pause nil       :stop nil}
                 "waiting"  {:play :disabled :pause :disabled :stop :disabled}
                 "paused"   {:play nil       :pause :disabled :stop nil}
                 "error"    {:play :disabled :pause :disabled :stop :disabled}
                 "complete" {:play :disabled :pause :disabled :stop :disabled}
                 "removed"  {:play :disabled :pause :disabled :stop :disabled}
                 "linked"   {:play :disabled :pause :disabled :stop :disabled})]
      [h-box
       :children [(or [pause-button (:pause dis?) download])
                  (or [play-button  (:play dis?)  download])
                  (or [stop-button  (:stop dis?)  download])]])))

(defn status [{:keys [status]}]
  [:span status])

(defn component [gid]
  (let [download (subscribe [:download gid])]
    (fn [gid]
      (let [dl @download]
        [v-box
         :style {:position "relative"}
         :children [[filename dl]
                    [status dl]
                    [controls dl]
                    [speeds dl]
                    [:pre (pr-str @(subscribe [:download gid]))]
                    [progress dl]]]))))
