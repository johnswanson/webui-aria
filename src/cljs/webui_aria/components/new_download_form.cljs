(ns webui-aria.components.new-download-form
  (:require-macros [webui-aria.macros :refer [handler-fn]])
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as com]
            [reagent.core :as reagent]
            [webui-aria.style :as style]
            [clojure.string :as str]
            [re-com.popover :refer [popover-content-wrapper]]
            [webui-aria.components.modern-button :as modern-button]))

(defn new-download-form-start-button [val]
  [modern-button/view
   :label [:span "Start!"]
   :style {:font-size "15px"}
   :on-click (handler-fn
              (let [str-val @val
                    lines (str/split str-val #"\n")]
                (dispatch [:add-uri {:uris lines}])
                (dispatch [:new-download-form-hide])
                (reset! val "")))])

(defn new-download-form-textarea [val]
  [com/input-textarea
   :model       @val
   :on-change   (handler-fn (reset! val event))
   :width       "800px"
   :placeholder "URLs (or magnet torrent links) to download, separated by newlines"
   :rows        5])

(defn view [showing?]
  (let [val (reagent/atom "")]
    (fn [showing?]
      [popover-content-wrapper
       :showing? showing?
       :position :right-below
       :no-clip? true
       :body [com/v-box
              :gap "1em"
              :children [[new-download-form-textarea val]
                         [com/h-box
                          :justify :center
                          :children [[new-download-form-start-button val]]]]]
       :on-cancel #(dispatch [:new-download-form-hide])])))
