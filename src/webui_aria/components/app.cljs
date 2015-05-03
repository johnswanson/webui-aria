(ns webui-aria.components.app
  (:require [webui-aria.components.downloads :as downloads]
            [webui-aria.actions :as actions]
            [webui-aria.utils :as utils]
            [reagent.core :as reagent :refer [atom cursor]]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn nav-component [api pub]
  (fn [api pub]
    [:nav
     [:div.nav-wrapper
      [:div.container
       [:a.brand-logo {:href "#"} "Aria WebUI"]
       [:ul.right
        [:li [:a.nav {:href "#"} "Configure"]]]]]]))

(defn new-download-component [actions]
  (fn [actions]
    [:a.btn
     {:on-click #(actions/emit-adding-new-download! actions)}
     "New Download"]))

(defn filter-selector [filter? filter-name actions]
  (let [id (utils/rand-hex-str 16)]
    (fn [filter? filter-name actions]
      [:div.row
       [:input {:type "checkbox"
                :id id
                :on-change #(actions/emit-filter-toggled!
                             actions
                             filter-name
                             (not @filter?))
                :checked @filter?}]
       [:label {:for id} (name filter-name)]])))

(defn filters-component [filters actions]
  (fn [filters actions]
    (let [{:keys [running? active?]} @filters]
      [:div
       [filter-selector (cursor filters [:running]) :running actions]
       [filter-selector (cursor filters [:active]) :active actions]
       [filter-selector (cursor filters [:waiting]) :waiting actions]
       [filter-selector (cursor filters [:complete]) :complete actions]
       [filter-selector (cursor filters [:error]) :error actions]
       [filter-selector (cursor filters [:paused]) :paused actions]
       [filter-selector (cursor filters [:removed]) :removed actions]
       [filter-selector (cursor filters [:linked]) :linked actions]])))

(defn listen-for-filters! [filters pub]
  (let [ch (a/chan)]
    (a/sub pub :filter-toggled ch)
    (go-loop []
      (let [[action-type {:keys [filter active?]}] (a/<! ch)]
        (swap! filters assoc filter active?)
        (recur)))))

(defn app [api pub actions]
  (fn [api pub actions]
    (let [filters (atom {:running true
                         :active true
                         :waiting true
                         :complete true
                         :error true
                         :paused true
                         :removed true
                         :linked false})]
      (listen-for-filters! filters pub)
      [:div.entire-app
       [:header
        [:div.navbar-fixed
         [nav-component api pub]]]
       [:div.row.content
        [:div.col.s2.sidebar
         [:div.container
          [:div.row.section
           [:div.col.s12 [new-download-component actions]]]
          [:div.row
           [:div.col.s12 [new-download-component actions]]]]
         [:div.container
          [:div.row.section
           [:div.col.s12 [filters-component filters actions]]]]]
        [:main
         [:div.col.s10.offset-s2.offset-m0.main
          [downloads/downloads-component filters api pub]]]]
       [:footer.page-footer]])))
