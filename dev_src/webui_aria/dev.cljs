(ns webui-aria.dev
    (:require
     [webui-aria.core]
     [webui-aria.utils :refer [hostname]]
     [figwheel.client :as fw]))

(fw/start {
  :websocket-url (str "ws://" hostname ":3449/figwheel-ws")
  :on-jsload (fn []
               ;; (stop-and-start-my app)
               )})
