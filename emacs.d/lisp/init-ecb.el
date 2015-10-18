(require-package 'ecb)

(custom-set-variables
 '(ecb-options-version "2.40"))

(setq ecb-layout-name "left15")
(setq ecb-source-in-directories-buffer 'always)
(setq ecb-use-speedbar-instead-native-tree-buffer 'dir)

(setq ecb-auto-activate t)
(setq ecb-tip-of-the-day nil)
(provide 'init-ecb)

;; M-x ecb-activate
;; M-x ecb-deactivate
;; M-x ecb-minor-mode (de)activate

;; window switch
;; C-c . g h ;; goto history window
;; C-c . g m ;; goto methods window
;; C-c . g s ;; goto sources window
;; C-c . g d ;; goto directories window
;; C-c . g 1 ;; goto main buffer
