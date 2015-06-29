(ns webui-aria.components.download
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [re-frame.utils :refer [log warn error]]
            [reagent.core :as reagent]
            [cljs.core.async :refer [timeout <! put! chan]]))

(defn progress [dl]
  (fn [dl]
    (let [pct (* 100 (/ (:completed-length dl) (:total-length dl)))]
      [:div {:style {:position "absolute"
                     :top "0px"
                     :left "0px"
                     :z-index "-1"
                     :height "100%"
                     :background-color "#DDD"
                     :width (str pct "%")}}])))

(defn component [gid]
  (let [download (subscribe [:download gid])
        stop-ch (chan)
        start!  (fn [download]
                  (go-loop []
                    (let [t (timeout 1000)
                          [_ ch] (alts! [t stop-ch])]
                      (when (= ch t)
                        (dispatch [:get-status {:gid gid}])
                        (recur)))))
        stop!   (fn [] (put! stop-ch nil))]
    (reagent/create-class
     {:component-did-mount (partial start! download)
      :component-will-unmount stop!
      :reagent-render
      (fn [gid]
        (let [dl @download]
          [:div {:style {:position "relative"}}
           [:span (or (-> dl :files first :path) (get-in dl [:bittorrent :info :name]))]
           [:pre (pr-str @(subscribe [:download gid]))]
           [progress dl]]))})))
