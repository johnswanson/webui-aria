(ns webui-aria.components.app
  (:require [webui-aria.components.downloads :as downloads]
            [reagent.core :as reagent :refer [atom]]))

(defn app [api pub]
  [:div.container
   [downloads/downloads-component api pub]])
