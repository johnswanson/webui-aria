(ns ^:figwheel-always webui-aria.core
    (:require [reagent.core :as reagent]
              [re-frame.core :refer [dispatch-sync dispatch]]
              [webui-aria.components.app :refer [app]]
              [webui-aria.subs]
              [webui-aria.handlers])
    (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defonce update! (js/setInterval #(dispatch [:api-get-all]) 1000))

(dispatch-sync [:init-db])
(dispatch-sync [:api-connect {:hostname "localhost"}])
(reagent/render-component
 [app]
 (.-body js/document))

