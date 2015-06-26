(ns webui-aria.handlers
  (:require [plumbing.core :refer :all]
            [re-frame.core :as re-frame]
            [webui-aria.db :as db]))

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))
