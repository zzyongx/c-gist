;; eval-buffer
(add-to-list 'load-path (expand-file-name "lisp" user-emacs-directory))
(require 'init-benchmarking)

(require 'init-compat)
(require 'init-utils)
(require 'init-site-lisp) ;; Must come before elpa, as it provided package.el
(require 'init-elpa)      ;; Machinery for install required package

(require-package 'wgrep)
(require-package 'project-local-variables)

(require 'init-ido)
(require 'init-auto-complete)
;;(require 'init-cedet)
(require 'init-ecb)
(require 'init-markdown)
(require 'init-javascript)
(require 'init-lua)

;; line number
(global-linum-mode t)

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

;; C-x <SPC> rect-mark-mode
;; C-x r k   kill rect
;; C-x r M-W copy rect
;; C-x r d   del  rect
;; C-x r y   yank rect
;; C-x r o   insert rect spac
;; C-x r t *string* <RET> replace rect

;; move around window
(global-set-key [M-left]  'windmove-left)
(global-set-key [M-right] 'windmove-right)
(global-set-key [M-up]    'windmove-up)
(global-set-key [M-down]  'windmove-down)
