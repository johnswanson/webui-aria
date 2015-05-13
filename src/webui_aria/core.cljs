(ns ^:figwheel-always webui-aria.core
    (:require [cljs.core.async :refer [chan pub <! >! put! close! sub]]
              [reagent.core :as reagent]
              [webui-aria.actions :as actions]
              [webui-aria.components.app :refer [app]]
              [webui-aria.notifications :as notifications])
    (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(notifications/request!)

(def action-chan (chan))
(def action-pub (pub action-chan actions/type))

(reagent/render-component
 [app action-pub action-chan]
 (.-body js/document))

