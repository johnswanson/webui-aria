(ns webui-aria.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
 :active-panel
 (fn [db _]
   (reaction (:active-panel @db))))

(re-frame/register-sub
 :connection
 (fn [db]
   (reaction (:connection @db))))

(re-frame/register-sub
 :download-gids
 (fn [db]
   (reaction (keys (:downloads @db)))))

(re-frame/register-sub
 :download
 (fn [db [_ gid]]
   (reaction (get (:downloads @db) gid))))

(re-frame/register-sub
 :pending-requests
 (fn [db]
   (reaction (:pending-requests @db))))
