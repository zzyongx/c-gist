(require 'cc-mode)
(setq-default c-basic-offset 2 c-default-style "linux")
(setq-default tab-width 2)

(define-key c-mode-base-map (kbd "RET") 'newline-and-indent)
(add-to-list 'auto-mode-alist '("\\.h\\'" . c++-mode))

;; (add-hook 'find-file-hook 'flymake-find-file-hook)
;; (global-set-key (kbd "M-g M-n") 'flymake-goto-next-error)
;; (global-set-key (kbd "M-g M-p") 'flymake-goto-pre-error)

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
             (setq c-basic-offset tab-width)
             (setq compile-command "make-c"))
          )

;; java mode
(add-hook 'java-mode-hook
          '(lambda ()
            (java-mode-indent-annotations-setup)
            (setq compile-command "make-java"))
          )

;; M-x compile
;; go mode
(add-hook 'go-mode-hook
          '(lambda ()
            (setq compile-command "make-go"))
          )

;; compile window position
(defadvice compile (around split-horizontally activate)
  (let ((split-height-threshold 0)
        (split-width-threshold nil))
    ad-do-it))

(defun compilation-exit-autoclose (status code msg)
  ;; If M-x compile exists with a 0
  (when (and (eq status 'exit) (zerop code))
    ;; then bury the *compilation* buffer, so that C-x b doesn't go there
    (bury-buffer)
    ;; and delete the *compilation* window
    (delete-window (get-buffer-window (get-buffer "*compilation*"))))
  ;; Always return the anticipated result of compilation-exit-message-function
  (cons msg code))

;; Specify my function (maybe I should have done a lambda function)
(setq compilation-exit-message-function 'compilation-exit-autoclose)

;; C-x k  kill buffer

(provide 'init-cc)
