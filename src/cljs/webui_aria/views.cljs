(ns webui-aria.views
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [webui-aria.macros :refer [handler-fn]])
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [re-frame.utils :refer [log warn error]]
            [re-com.core :refer [h-split v-box button input-textarea h-box checkbox]]
            [re-com.popover :refer [popover-content-wrapper popover-anchor-wrapper]]
            [reagent.core :as reagent]
            [cljs.core.async :refer [timeout <! put! chan]]
            [webui-aria.components.download :as download]
            [clojure.string :as str]))

(defn request-render [request]
  [:pre (pr-str request)])

(defn downloads-panel []
  (let [dls (subscribe [:filtered-downloads])]
    (fn []
      [:div
       (for [dl @dls]
         ^{:key (:gid dl)} [download/component (:gid dl)])])))


(defn modern-button [& {:keys [label on-click]}]
  (let [hover? (reagent/atom false)]
    (fn [& {:keys [label on-click style]}]
      [button
       :label    label
       :on-click on-click
       :style    (merge {:color            "black"
                         :background-color (if @hover? "#ddd" "#fff")
                         :font-size        "22px"
                         :font-weight      "300"
                         :border           "1px solid black"
                         :border-radius    "0px"
                         :padding          "20px 26px"} style)
       :attr     {:on-mouse-over (handler-fn
                                  (reset! hover? true))
                  :on-mouse-out  (handler-fn
                                  (reset! hover? false))}])))

(defn new-download-form-start-button [val]
  [modern-button
   :label [:span "Start!"]
   :style {:font-size "15px"}
   :on-click (handler-fn
              (let [str-val @val
                    lines (str/split str-val #"\n")]
                (dispatch [:add-uri {:uris lines}])
                (dispatch [:new-download-form-hide])
                (reset! val "")))])

(defn new-download-form-textarea [val]
  [input-textarea
   :model       @val
   :on-change   (handler-fn (reset! val event))
   :width       "800px"
   :placeholder "URLs (or magnet torrent links) to download, separated by newlines"
   :rows        5])

(defn new-download-form [showing?]
  (let [val (reagent/atom "")]
    (fn [showing?]
      [popover-content-wrapper
       :showing? showing?
       :position :right-below
       :no-clip? true
       :body [v-box
              :gap "1em"
              :children [[new-download-form-textarea val]
                         [h-box
                          :justify :center
                          :children [[new-download-form-start-button val]]]]]
       :on-cancel #(dispatch [:new-download-form-hide])])))

(defn new-download-button []
  (let [showing? (subscribe [:new-download-form-showing?])
        content  (reagent/atom "")]
    (fn []
      [popover-anchor-wrapper
       :showing? showing?
       :position :right-below
       :anchor [modern-button
                :label [:span "New Download"]
                :on-click #(dispatch [:new-download-form-show])]
       :popover [new-download-form showing?]])))

(defn checkbox-for-filter [filt active?]
  [checkbox
   :model active?
   :on-change #(dispatch [:filter-toggled filt])
   :label (name filt)])

(defn filters []
  (let [filters (subscribe [:filters])]
    (fn []
      [v-box
       :children [(for [[filt active?] @filters]
                    ^{:key filt} [checkbox-for-filter filt active?])]])))

(defn buttons-and-filters []
  (fn []
    [v-box
     :children [[new-download-button]
                [filters]]]))

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
