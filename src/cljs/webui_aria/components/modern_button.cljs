(ns webui-aria.components.modern-button
  (:require-macros [webui-aria.macros :refer [handler-fn]])
  (:require [re-frame.core :refer [subscribe]]
            [re-com.core :as com]
            [reagent.core :as reagent]
            [webui-aria.style :as style]))

(defn view [& {:keys [label on-click]}]
  (let [hover? (reagent/atom false)]
    (fn [& {:keys [label on-click style]}]
      [com/button
       :label    label
       :on-click on-click
       :style    (merge {:color            "black"
                         :background-color (if @hover? "#ddd" "#fff")
                         :font-size        "22px"
                         :font-weight      "100"
                         :border           "1px solid black"
                         :border-radius    "0px"
                         :padding          "20px 26px"} style)
       :attr     {:on-mouse-over (handler-fn
                                  (reset! hover? true))
                  :on-mouse-out  (handler-fn
                                  (reset! hover? false))}])))
