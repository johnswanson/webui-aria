(ns webui-aria.views
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [re-frame.utils :refer [log warn error]]
            [reagent.core :as reagent]
            [cljs.core.async :refer [timeout <! put! chan]]
            [webui-aria.api :as api]))

;; --------------------

(defn download-render [gid]
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
        [:div
         [:h2 gid]
         [:pre (pr-str @(subscribe [:download gid]))]])})))

(defn request-render [request]
  [:pre (pr-str request)])

(defn home-panel []
  (let [gids     (subscribe [:download-gids])
        requests (subscribe [:pending-requests])]
    (fn []
      [:div
       (for [gid @gids]
         ^{:key gid} [download-render gid])
       (into [:div] (map request-render @requests))])))

(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a {:href "#/"} "go to Home Page"]]]))

;; --------------------
(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :about-panel [] [about-panel])
(defmethod panels :default [] [:div])

(defn main-panel []
  (let [active-panel (subscribe [:active-panel])]
    (fn []
      (panels @active-panel))))
