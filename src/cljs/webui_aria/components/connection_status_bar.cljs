(ns webui-aria.components.connection-status-bar
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as com]
            [clojure.string :as str]
            [webui-aria.style :as style]
            [webui-aria.components.connection-config-button :as connection-config-button]))


(defn connection-status-label-style [connected?]
  {:font-family "'Roboto', sans-serif"
   :font-weight "100"
   :font-size "24px"})

(defn connection-status-bar-style [connected?]
  {:background-color (if connected? style/base02 style/base03)
   :transition "all .2s ease-in"
   :color (if connected? style/cyan style/red)})

(defn connection-status-label [str connected?]
  [com/label :style (connection-status-label-style connected?) :label str])

(defn connection-status-bar* [connection-status]
  (let [connected? (= connection-status :connected)
        connected-str (if connected? "Connected" "Disconnected")]
    [com/h-box
     :gap "1em"
     :justify :end
     :children [(when (= connection-status :connecting) [com/throbber
                                                         :size :small
                                                         :color style/red])
                [connection-status-label connected-str connected?]
                [com/gap :size "10px"]
                [connection-config-button/view]
                [com/gap :size "150px"]]
     :style (connection-status-bar-style connected?)]))

(defn view []
  (let [connection-status (subscribe [:api-connection-status])]
    (fn []
      [connection-status-bar* @connection-status])))
