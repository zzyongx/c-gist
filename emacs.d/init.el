;; usage
;; ln -s PATH/.emacs.d ~/.emacs.d
;; emacs --version 26.3

;; ielm
;; describe-bindings # M-:
;; apropos # symbol search
;; C-x C-e eval elisp expression

(let ((minver "26.3"))
  (when (version< emacs-version minver)
    (error "Your Emacs is too old -- this config requires v%s or higher" minver)))

(setq debug-on-error t)

;; C-M-n forward-list Move forward over a parenthetical group
;; C-M-p backward-list Move backward over a parenthetical group
;; C-M-f forward-sexp Move forward over a balanced expression
;; C-M-b backward-sexp Move backward over a balanced expression
;; C-M-k kill-sexp Kill balanced expression forward
;; C-M-SPC mark-sexp Put the mark at the end of the sexp.

;; env
(setenv "PATH" (concat (file-name-directory user-init-file) "bin:" (getenv "PATH")))
(setq startup-directory default-directory)
(setq warning-minimum-level :error)

(add-to-list 'load-path (expand-file-name "lisp" user-emacs-directory))
(add-to-list 'load-path (expand-file-name "repo" user-emacs-directory))
(require 'init-benchmarking) ;; Measure startup time

(defconst *spell-check-support-enabled* nil) ;; Enable with t if you prefer
(defconst *is-a-mac* (eq system-type 'darwin))

;;----------------------------------------------------------------------------
;; Adjust garbage collection thresholds during startup, and thereafter
;;----------------------------------------------------------------------------
(let ((normal-gc-cons-threshold (* 20 1024 1024))
      (init-gc-cons-threshold (* 128 1024 1024)))
  (setq gc-cons-threshold init-gc-cons-threshold)
  (add-hook 'emacs-startup-hook
            (lambda () (setq gc-cons-threshold normal-gc-cons-threshold))))

;;----------------------------------------------------------------------------
;; Bootstrap config
;;----------------------------------------------------------------------------
(setq custom-file (expand-file-name "custom.el" user-emacs-directory))
(require 'init-utils)
(require 'init-site-lisp) ;; Must come before elpa, as it may provide package.el
;; Calls (package-initialize)
(require 'init-elpa)      ;; Machinery for installing required packages
(require 'init-exec-path) ;; Set up $PATH

;; case sensitive
(setq case-fold-search nil)
(setq case-replace nil)

;; line number
(global-linum-mode t)
(setq column-number-mode t)

;; emacs 26.1
;; (global-display-line-numbers-mode t)
;; (setq display-line-numbers 'relative)

;; syntax on
(global-font-lock-mode t)

;; parentheses match
(show-paren-mode t)
(setq show-paren-style 'parentheses)

;; y or n
(fset 'yes-or-no-p 'y-or-n-p)

;; cancel backup file, temp file
(setq make-backup-file nil)
(setq-default make-backup-files nil)

;; save place
(require 'saveplace)
(setq-default save-place t)

;; M-x delete-trailing-whitespace
(add-hook 'before-save-hook 'delete-trailing-whitespace)

;; imenu
(global-set-key [(control c)(i)] 'imenu)
;; M-. find tag
;; C-u M-. go back to previous tag foudn
;; M-* go back to previous invoked M-.

;; mark
;; C-@ C-x C-x
;; M-@ to end of word
;; M-h whole paragraph
;; C-M-h whole defun
;; C-x h whole buffer

;; C-w kill region
;; M-w copy region
;; C-y yank
;; C-x k kill buffer

;; C-x <SPC> rect-mark-mode
;; C-x r k   kill rect
;; C-x r M-W copy rect
;; C-x r d   del  rect
;; C-x r y   yank rect
;; C-x r o   insert rect spac
;; C-x r t *string* <RET> replace rect

;; C-u C-@ back to last position
;; C-x o switch between buffer

;; comment/uncomment
;; M-:

;; F10 toggle menu

;; coding system
;; M-x describe-coding-system
;; M-x universal-coding-system-argument

;; M-x org-version
;; M-x set-buffer-file-coding-system utf-8-unix

;; space to tab/tab to space
;; M-x tabify
;; M-x untabify

;; search & replace
;; M-x count-matches

;; move around window
(global-set-key [M-left]  'windmove-left)
(global-set-key [M-right] 'windmove-right)
(global-set-key [M-up]    'windmove-up)
(global-set-key [M-down]  'windmove-down)

(defun other-window-backward (&optional n)
  (interactive "P")
  (other-window (- (prefix-numeric-value n))))

(global-set-key (kbd "C-x C-n") 'other-window)
(global-set-key (kbd "C-x C-p") 'other-window-backward)

;; sh-mode
(add-hook 'sh-mode-hook
          '(lambda()
             (setq sh-basic-offset 2)
             (setq sh-indentation 2))
          )

(require-package 'sr-speedbar)
(require 'sr-speedbar)
(setq sr-speedbar-right-side nil)
(global-set-key (kbd "M-s M-s") 'sr-speedbar-toggle)

;; diff
(setq ediff-split-window-function 'split-window-horizontally)

;; modules
(require-package 'wgrep)
(require-package 'magit)
(require-package 'flycheck)

;; (require 'init-compat)
(require 'init-utils)
(require 'init-ido)
(require 'init-auto-complete)
(require 'init-cedet)
(require 'init-yasnippet)
(require 'init-imenu)
(require 'java-mode-indent-annotations)
(require 'init-cc)
(require 'init-perl)
(require 'init-javascript)
(require 'init-typescript)
(require 'init-lua)
(require 'init-golang)
(require 'init-groovy)
(require 'init-php)
(require 'init-python)
(require 'init-markdown)
(require 'init-rust)

(require 'init-org)
(require 'init-ox-reveal)
(require 'init-dot)
(require 'init-yaml)

(require 'init-shell)

;; IDE
(require 'init-ggtags)

;; eof
(custom-set-variables
 ;; custom-set-variables was added by Custom.
 ;; If you edit it by hand, you could mess it up, so be careful.
 ;; Your init file should contain only one such instance.
 ;; If there is more than one, they won't work right.
 '(comint-buffer-maximum-size 20000)
 '(comint-completion-addsuffix t)
 '(comint-get-old-input (lambda nil "") t)
 '(comint-input-ignoredups t)
 '(comint-input-ring-size 5000)
 '(comint-move-point-for-output nil)
 '(comint-prompt-read-only nil)
 '(comint-scroll-show-maximum-output t)
 '(comint-scroll-to-bottom-on-input t)
 '(org-emphasis-alist
   (quote
    (("*" bold)
     ("/" italic)
     ("_" underline)
     ("=" org-verbatim verbatim)
     ("~" org-code verbatim)
     ("+"
      (:strike-through t)))))
 '(package-selected-packages
   (quote
    (ggtags yaml-mode graphviz-dot-mode org-re-reveal htmlize rust-mode markdown-mode jedi elpy php-mode groovy-mode go-mode lua-mode add-node-modules-path skewer-mode js-comint prettier-js typescript-mode coffee-mode js2-mode json-mode imenu-list yasnippet auto-complete idomenu smex flycheck magit wgrep sr-speedbar gnu-elpa-keyring-update fullframe seq vue-mode rainbow-delimiters haskell-mode ac-js2)))
 '(protect-buffer-bury-p nil)
 '(tramp-default-method "ssh"))
(custom-set-faces
 ;; custom-set-faces was added by Custom.
 ;; If you edit it by hand, you could mess it up, so be careful.
 ;; Your init file should contain only one such instance.
 ;; If there is more than one, they won't work right.
 )
