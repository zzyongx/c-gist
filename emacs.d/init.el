;; usage
;; ln -s PATH/.emacs.d ~/.emacs.d
;; mkdir ~/.emcas/site-lisp

;; ielm
;; describe-bindings # M-:
;; apropos # symbol search
;; C-x C-e eval elisp expression

;; (setq debug-on-error t)

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

;; eval-buffer
(add-to-list 'load-path (expand-file-name "lisp" user-emacs-directory))
(require 'init-benchmarking)

;; case sensitive
(setq case-fold-search nil)
(setq case-replace nil)

(require 'init-compat)
(require 'init-utils)
;; disable auto package upgrade to speed up startup
(setq require-package-at-startup 0)
(require 'init-site-lisp) ;; Must come before elpa, as it provided package.el
(require 'init-elpa)      ;; Machinery for install required package

(require-package 'wgrep)
(require-package 'magit)

(require 'init-ido)
(require 'init-auto-complete)
(require 'init-cedet)
(require 'init-yasnippet)
(require 'init-imenu)
(require 'java-mode-indent-annotations)
(require 'init-cc)
(require 'init-perl)
(require 'init-javascript)
(require 'init-lua)
(require 'init-golang)
(require 'init-groovy)
(require 'init-php)
(require 'init-python)
(require 'init-haskell)

(require 'init-org)
(require 'init-ox-reveal)
(require 'init-dot)
(require 'init-yaml)

(require 'init-shell)

;; IDE
(require 'init-ggtags)

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
;; (add-hook 'c-mode-hook
;;           (lambda () (add-to-list 'write-file-functions 'delete-trailing-whitespace)))

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

(defun foo (string)
  (interactive)
  '(string))

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
