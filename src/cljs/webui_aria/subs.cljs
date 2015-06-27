(ns webui-aria.subs
    (:require-macros [reagent.ratom :refer [reaction]]
                     [plumbing.core :refer [defnk fnk]])
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
 :downloads
 (fn [db]
   (reaction (:downloads @db))))

(re-frame/register-sub
 :pending-requests
 (fn [db]
   (reaction (:pending-requests @db))))
