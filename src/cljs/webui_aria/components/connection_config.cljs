(ns webui-aria.components.connection-config
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as com]
            [re-com.popover :refer [popover-content-wrapper]]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [webui-aria.style :as style]))

(defn connection-config-input [kw regex]
  (let [old-val     (subscribe [kw])
        val         (atom @old-val)
        on-change   #(dispatch [(keyword (str (name kw) "-changed")) %])
        on-key-down #(case (.-which %)
                       13 (do
                            (on-change (-> % .-target .-value))
                            (dispatch [:connection-config-form-save-and-hide]))
                       27 (dispatch [:connection-config-form-hide])
                       nil)]
    (fn []
      [com/h-box
       :gap "2em"
       :children [[com/v-box :children [[com/label :label (name kw)]] :justify :center]
                  [com/gap :size "1"]
                  [com/input-text
                   :attr {:on-key-down on-key-down}
                   :model val
                   :validation-regex regex
                   :on-change on-change]]])))

(defn token-input []
  (connection-config-input :connection-token #".*"))

(def host-regex
  #"^(?=.{1,255}$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?(?:\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?)*\.?$")

(defn host-input []
  (connection-config-input :connection-host host-regex))

(defn port-input []
  (connection-config-input :connection-port #"^\d+$"))

(defn secure?-input []
  (let [old-val   (subscribe [:connection-secure?])
        val       (reagent/atom @old-val)
        on-change #(do
                     (reset! val %)
                     (dispatch [:connection-secure?-changed %]))]
    (fn []
      [com/h-box
       :gap "2em"
       :children [[com/v-box :children [[com/label :label "secure?"]] :justify :center]
                  [com/gap :size "1"]
                  [com/checkbox
                   :model val
                   :on-change on-change]]])))

(defn path-input []
  (connection-config-input :connection-path #"^/[-\w]*$"))

(defn view [showing?]
  [popover-content-wrapper
   :showing? showing?
   :position :below-left
   :body [com/v-box
          :gap "1em"
          :children [[token-input]
                     [host-input]
                     [port-input]
                     [secure?-input]
                     [path-input]]]
   :on-cancel #(dispatch [:connection-config-form-save-and-hide])])
