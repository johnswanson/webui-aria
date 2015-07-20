(ns webui-aria.components.downloads-panel
  (:require [re-frame.core :refer [subscribe]]
            [re-com.core :as com]
            [reagent.core :as reagent]
            [webui-aria.components.download :as download]
            [webui-aria.style :as style]))

(defn view []
  (let [dls (subscribe [:filtered-downloads])]
    (fn []
      [com/v-box
       :width "100%"
       :children [(for [dl @dls]
                    ^{:key (:gid dl)} [download/component (:gid dl)])]])))

