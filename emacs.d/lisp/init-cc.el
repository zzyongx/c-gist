;; cc
(add-hook 'c-mode-common-hook
          (lambda()
            (setq indent-tabs-mode nil)
            (set c-basic-offset 2)))

(require 'cc-mode)
(setq-default c-basic-offset 2 c-default-style "linux")
(setq-default tab-width 2 indent-tabs-mode t)
(define-key c-mode-base-map (kbd "RET") 'newline-and-indent)
(add-to-list 'auto-mode-alist '("\\.h\\'" . c++-mode))

(c-add-style "mycodingstyle"
             '((c-comment-only-line-offset . 0)
               (c-hanging-braces-alist . ((substatement-open before
                                                             after)))
               (c-offsets-alist . ((topmost-intro        . 0)
                                   (topmost-intro-cont   . 0)
                                   (substatement-open    . 0)
                                   (innamespace          . 0)
                                   ))))

;; c/c++ mode
(add-hook 'c-mode-common-hook
          '(lambda()
             (c-set-style "mycodingstyle")
             (setq tab-width 2)
             (setq c-basic-offset tab-width)))

(provide 'init-cc)
