(ns webui-aria.components.app
  (:require [webui-aria.components.downloads :as downloads]
            [webui-aria.utils :as utils]
            [re-frame.core :refer [dispatch subscribe]]))

(defn filter-selector [f]
  (let [id (utils/rand-hex-str 16)]
    (fn [{:keys [fname enabled?]}]
      [:div
       [:input {:type "checkbox"
                :id id
                :on-change #(dispatch [:filter-toggled fname])
                :checked enabled?}]
       [:label {:for id} (name fname)]])))

(defn filter-items []
  (let [filters (subscribe [:filters])]
    (fn []
      [:div [:h5 "Filters"]
       (for [f (vals @filters)]
         ^{:key (:fname f)} [filter-selector f])])))

(defn new-download-button []
  (fn []
    [:a.btn {:on-click #(dispatch [:open-new-download-modal])}
     "New Download"]))

(defn logo []
  (fn []
    [:a {:href "#"} "Aria WebUI"]))

(defn configure-button []
  (fn []
    [:a {:href "#"} "Configure"]))

(defn links []
  (fn [link-components]
    [:ul
     (for [i (range (count link-components))
           :let [link-component (get link-components i)]]
      ^{:key i} [:li [link-component]])]))

(defn app
  []
  (let [downloads (subscribe [:downloads])
        filtered  (subscribe [:filtered-downloads])]
    (fn []
      [:div
       [:section#app
        [:header#header
         [logo]
         [links [configure-button]]]
        [:section#sidebar
         [new-download-button]
         [filter-items]]
        [:main
         [downloads/download-items []]]]])))
