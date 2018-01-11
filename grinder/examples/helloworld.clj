;; Hello World in Clojure
;;
;; A simple Clojure script.
(let [grinder net.grinder.script.Grinder/grinder]

  ; The script returns a factory function, called once by each worker
  ; thread.
  (fn []

    ; The factory function returns test runner function.
    (fn []	
      (do
        (.. grinder (getLogger) (info "Hello World"))))))
