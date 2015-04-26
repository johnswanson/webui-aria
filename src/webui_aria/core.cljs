(ns ^:figwheel-always webui-aria.core
    (:require [cljs.core.async :refer [chan pub <! >! put! close! sub]]
              [reagent.core :as reagent]
              [webui-aria.actions :as actions]
              [webui-aria.api :refer [make-api]]
              [webui-aria.components.version :refer [version-component]]
              [webui-aria.components.download :refer [download-component]])
    (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(def action-chan (chan))
(def action-pub (pub action-chan actions/type))

(def api (make-api {:token "testing"} action-chan))

(def p (:pub api))

(reagent/render-component
 [download-component api action-pub]
 (.-body js/document))

