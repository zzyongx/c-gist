(require-package 'ggtags)
(add-hook 'c-mode-common-hook
          (lambda ()
            (when (derived-mode-p 'c-mode 'c++-mode 'java-mode)
              (ggtags-mode 1))))
(provide 'init-ggtags)

;; should install gnu global first

;; M-.     skip to definition
;; M-,     skip back
;; C-c M-p skip to last Mark
