(ns webui-aria.views
  (:require-macros [plumbing.core :refer [defnk fnk]])
  (:require [re-frame.core :as re-frame]
            [webui-aria.api :as api]))

;; --------------------

(defn download-render [download]
  [:pre (pr-str download)])

(defn request-render [request]
  [:pre (pr-str request)])

(defn home-panel []
  (let [downloads (re-frame/subscribe [:downloads])
        requests (re-frame/subscribe [:pending-requests])]
    (fn []
      [:div
       (into [:div] (map download-render @downloads))
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
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      (panels @active-panel))))
