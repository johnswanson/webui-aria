(ns webui-aria.components.connection-status-bar
  (:require [re-frame.core :refer [subscribe]]
            [re-com.core :as com]
            [clojure.string :as str]
            [webui-aria.style :as style]))

(defn connection-status-label-style [connected?]
  {:style {:font-family "'Roboto', sans-serif"
           :font-weight "100"
           :font-size "24px"}})

(defn connection-status-bar-style [connected?]
  {:background-color (if connected?
                       style/base02
                       style/base1)
   :transition "all .5s ease-in"
   :color (if connected? style/magenta style/orange)})

(defn connection-status-label [str connected?]
  [:span (connection-status-label-style connected?) str])

(defn connection-status-bar* [connection-status]
  (let [connected? (= connection-status :connected)
        color (if connected?
                "#E73356"
                "#266C73")
        connected-str (if connected? "Connected" "Disconnected")]
    [com/h-box
     :gap "1em"
     :justify :end
     :children [(when (= connection-status :connecting) [com/throbber
                                                         :size :small
                                                         :color color])
                [connection-status-label connected-str connected?]
                [com/md-icon-button :md-icon-name "md-settings"]]
     :height "3em"
     :style (connection-status-bar-style connected?)]))

(defn view []
  (let [connection-status (subscribe [:api-connection-status])]
    (fn []
      [connection-status-bar* @connection-status])))
