(ns webui-aria.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [plumbing.core :refer :all]
              [webui-aria.handlers]
              [webui-aria.subs]
              [webui-aria.routes :as routes]
              [webui-aria.views :as views]))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init [] 
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
