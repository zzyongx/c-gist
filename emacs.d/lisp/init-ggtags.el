(require-package 'ggtags)
(add-hook 'c-mode-common-hook
          (lambda ()
            (when (derived-mode-p 'c-mode 'c++-mode 'java-mode)
              (ggtags-mode 1))))
(provide 'init-ggtags)

;; M-.     skip to definition
;; C-c M-p skip to last Mark
