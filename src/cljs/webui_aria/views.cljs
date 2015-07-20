(ns webui-aria.views
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [webui-aria.macros :refer [handler-fn]])
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [re-frame.utils :refer [log warn error]]
            [re-com.core :as com]
            [reagent.core :as reagent]
            [cljs.core.async :refer [timeout <! put! chan]]
            [webui-aria.components.connection-status-bar :as connection-status-bar]
            [webui-aria.components.control-panel :as control-panel]
            [webui-aria.components.downloads-panel :as downloads-panel]
            [webui-aria.style :as style]
            [clojure.string :as str]))

(defn home-panel []
  (let [stop-ch (chan)
        start!  (fn []
                  (go-loop []
                    (let [t (timeout 1000)
                          [_ ch] (alts! [t stop-ch])]
                      (when (= ch t)
                        (dispatch [:tell-active])
                        (dispatch [:tell-waiting {:offset 0 :num 100}])
                        (dispatch [:tell-stopped {:offset 0 :num 100}])
                        (recur)))))
        stop!   (fn [] (put! stop-ch :stopped))]
    (reagent/create-class
     {:component-did-mount start!
      :component-will-unmount stop!
      :reagent-render
      (fn []
        [com/v-box
         :children [[connection-status-bar/view]
                    [com/h-split
                     :panel-1 [control-panel/view]
                     :panel-2 [downloads-panel/view]
                     :initial-split 20]]])})))

;; --------------------
(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :default [] [:div])

(defn main-panel []
  (let [active-panel (subscribe [:active-panel])]
    (fn []
      (panels @active-panel))))
