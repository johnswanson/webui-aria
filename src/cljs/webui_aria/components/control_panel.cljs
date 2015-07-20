(ns webui-aria.components.control-panel
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as com]
            [re-com.popover :refer [popover-anchor-wrapper]]
            [reagent.core :as reagent]
            [webui-aria.style :as style]
            [webui-aria.components.modern-button :as modern-button]
            [webui-aria.components.new-download-form :as new-download-form]))

(defn checkbox-for-filter [filt active?]
  [com/checkbox
   :model active?
   :on-change #(dispatch [:filter-toggled filt])
   :label (name filt)])

(defn filters []
  (let [filters (subscribe [:filters])]
    (fn []
      [com/v-box
       :children [(for [[filt active?] @filters]
                    ^{:key filt} [checkbox-for-filter filt active?])]])))

(defn new-download-button []
  (let [showing? (subscribe [:new-download-form-showing?])
        content  (reagent/atom "")]
    (fn []
      [popover-anchor-wrapper
       :showing? showing?
       :position :right-below
       :anchor [modern-button/view
                :label [:span "New Download"]
                :on-click #(dispatch [:new-download-form-show])]
       :popover [new-download-form/view showing?]])))

(defn view []
  [com/h-box
   :justify :center
   :children [[com/v-box
               :children [[new-download-button]
                          [filters]]]]])

