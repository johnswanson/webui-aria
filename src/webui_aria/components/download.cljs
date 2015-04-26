(ns webui-aria.components.download
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent :refer [atom]]
            [webui-aria.api :refer [start-download]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn download [api pub state]
  (let [ch (a/chan)]
    (go
      (let [resp (<! ch)]
        (swap! state assoc :download-progress resp)
        (a/unsub pub :DOWNLOAD-STARTED ch)))
    (a/sub pub :DOWNLOAD-STARTED ch)
    (start-download api (:url @state))))

(defn download-component [api pub]
  (let [state (atom nil)]
    (fn []
      [:div [:p "Download"]
       [:p (str "state: " @state)]
       [:input {:value (:url @state)
                :on-change #(swap! state assoc :url (-> % .-target .-value))}
        (:url @state)]
       [:button {:on-click #(download api pub state)} "Start Download"]])))
