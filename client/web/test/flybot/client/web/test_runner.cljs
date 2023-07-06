(ns flybot.client.web.test-runner
  (:require
    [figwheel.main.testing :refer-macros [run-tests-async]]
    ;; require all the namespaces that have tests in them
    [flybot.client.web.core.db-test]
    [flybot.client.web.core.dom.common.link-test]
    [flybot.client.web.core.dom.page.post-test]))

(defn -main [& args]
  ;; this needs to be the last statement in the main function so that it can
  ;; return the value `[:figwheel.main.async-result/wait 10000]`
  (run-tests-async 10000))