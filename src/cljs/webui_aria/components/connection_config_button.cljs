(ns webui-aria.components.connection-config-button
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as com]
            [re-com.popover :refer [popover-anchor-wrapper]]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [webui-aria.style :as style]
            [webui-aria.components.connection-config :as connection-config]))

(defn view []
  (let [showing? (subscribe [:connection-config-form-showing?])
        content  (subscribe [:connection])]
    (fn []
      [popover-anchor-wrapper
       :showing? showing?
       :position :below-left
       :anchor   [com/md-icon-button
                  :md-icon-name "md-settings"
                  :size :larger
                  :on-click #(dispatch [:connection-config-form-show])]
       :popover  [connection-config/view showing?]])))
