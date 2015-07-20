(ns webui-aria.components.connection-config
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as com]
            [re-com.popover :refer [popover-content-wrapper]]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [webui-aria.style :as style]))

(defn connection-config-input [kw regex]
  (let [old-val (subscribe [kw])
        val     (atom @old-val)]
    (fn []
      [com/h-box
       :gap "2em"
       :children [[com/label
                   :label (name kw)]
                  [com/input-text
                   :model val
                   :validation-regex regex
                   :on-change #(dispatch [(keyword (str (name kw) "-changed")) %])]]])))

(defn token-input []
  (connection-config-input :connection-token #".*"))

(defn host-input []
  (connection-config-input :connection-host #"^(?=.{1,255}$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?(?:\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?)*\.?$"))

(defn port-input []
  (connection-config-input :connection-port #"^\d+$"))

(defn secure?-input []
  (connection-config-input :connection-secure? #".*"))

(defn path-input []
  (connection-config-input :connection-path #"^/[-\w]*$"))

(defn view [showing?]
  [popover-content-wrapper
   :showing? showing?
   :position :below-left
   :no-clip? true
   :body [com/v-box
          :gap "1em"
          :children [[token-input]
                     [host-input]
                     [port-input]
                     [secure?-input]
                     [path-input]]]
   :on-cancel #(dispatch [:connection-config-form-hide])])
