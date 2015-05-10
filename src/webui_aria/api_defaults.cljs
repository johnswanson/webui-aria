(ns webui-aria.api-defaults)

(def defaults {:hostname (.-hostname (.-location js/window))
               :port "6800"
               :secure? nil
               :path "/jsonrpc"
               :token "testing"})
