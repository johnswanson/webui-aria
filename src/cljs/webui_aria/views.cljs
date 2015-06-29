(ns webui-aria.views
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [webui-aria.macros :refer [handler-fn]])
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [re-frame.utils :refer [log warn error]]
            [re-com.core :refer [h-split v-box button input-textarea]]
            [re-com.popover :refer [popover-content-wrapper popover-anchor-wrapper]]
            [reagent.core :as reagent]
            [cljs.core.async :refer [timeout <! put! chan]]
            [webui-aria.components.download :as download]
            [clojure.string :as str]))

(defn request-render [request]
  [:pre (pr-str request)])

(defn downloads-panel []
  (let [gids (subscribe [:download-gids])]
    (fn []
      [:div
       (for [gid @gids]
         ^{:key gid} [download/component gid])])))

(defn modern-button [& {:keys [label on-click]}]
  (let [hover? (reagent/atom false)]
    (fn [& {:keys [label on-click]}]
      [button
       :label label
       :on-click on-click
       :style    {:color            "white"
                  :background-color (if @hover? "#0072bb" "#4d90fe")
                  :font-size        "22px"
                  :font-weight      "300"
                  :border           "none"
                  :border-radius    "0px"
                  :padding          "20px 26px"}
       :attr     {:on-mouse-over (handler-fn
                                  (reset! hover? true))
                  :on-mouse-out  (handler-fn
                                  (reset! hover? false))}])))

(defn new-download-form []
  (let [val (reagent/atom "")]
    (fn []
      [:div.new-download-form
       [input-textarea
        :model       @val
        :on-change   (handler-fn (reset! val event))
        :placeholder "URLs (or magnet torrent links) to download, separated by newlines"
        :rows        5]
       [modern-button
        :label [:span "Start!" [:i.md-file-download]]
        :on-click (handler-fn
                   (let [str-val @val
                         lines (str/split str-val #"\n")]
                     (dispatch [:add-uri {:uris lines}])
                     (reset! val "")))]])))

(defn new-download-button []
  (let [showing? (reagent/atom false)
        content  (reagent/atom "")]
    (fn []
      [popover-anchor-wrapper
       :showing? showing?
       :position :right-below
       :anchor [modern-button
                :label [:span "New Download" [:i.md-file-download]]
                :on-click (handler-fn (reset! showing? true))]
       :popover [popover-content-wrapper
                 :showing? showing?
                 :position :right-below
                 :no-clip? true
                 :body [new-download-form]
                 :on-cancel (handler-fn (reset! showing? false))]])))

(defn buttons-and-filters []
  (fn []
    [v-box
     :children [[new-download-button]]]))

(defn home-panel []
  (fn []
    [h-split
     :panel-1 [buttons-and-filters]
     :panel-2 [downloads-panel]
     :initial-split 20]))

(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a {:href "#/"} "go to Home Page"]]]))

;; --------------------
(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :about-panel [] [about-panel])
(defmethod panels :default [] [:div])

(defn main-panel []
  (let [active-panel (subscribe [:active-panel])]
    (fn []
      (panels @active-panel))))
