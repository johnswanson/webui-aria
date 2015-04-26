(ns webui-aria.components.version
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent :refer [atom]]
            [webui-aria.api :refer [get-version]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn version [api pub state]
  (let [ch (a/chan)]
    (go
      (let [[_ {result "result"}] (<! ch)]
        (reset! state result)
        (a/unsub pub :VERSION-RECEIVED ch)))
    (a/sub pub :VERSION-RECEIVED ch)
    (get-version api)))

(defn version-component [api pub]
  (let [state (atom nil)]
    (fn []
      [:div [:p "I am a component"]
       [:p (str "contents: " @state)]
       [:button {:on-click #(version api pub state)} "Get Version"]])))
