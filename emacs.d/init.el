;; usage
;; ln -s PATH/.emacs.d ~/.emacs.d
;; mkdir ~/.emcas/site-lisp

;; ielm
;; describe-bindings # M-:
;; apropos # symbol search
;; C-x C-e eval elisp expression

;; (setq debug-on-error t)

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
(require 'init-cedet)
(require 'init-ecb)
(require 'init-yasnippet)
(require 'init-imenu)
(require 'java-mode-indent-annotations)
(require 'init-cc)
(require 'init-perl)
(require 'init-javascript)
(require 'init-lua)
(require 'init-scala)
(require 'init-groovy)
(require 'init-php)
(require 'init-python)
(require 'init-haskell)

(require 'init-org)
(require 'init-ox-reveal)
(require 'init-dot)

;; IDE
(require 'init-ggtags)

;; line number
(global-linum-mode t)
(setq column-number-mode t)

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

;; shell mode
(autoload 'ansi-color-for-comint-mode-on "ansi-color" nil t)
(add-hook 'shell-mode-hook 'ansi-color-for-comint-mode-on t)

;; sh-mode
(add-hook 'sh-mode-hook
          '(lambda()
             (setq sh-basic-offset 2)
             (setq sh-indentation 2)
             )
          )

(require-package 'sr-speedbar)
(require 'sr-speedbar)
(setq sr-speedbar-right-side nil)
(global-set-key (kbd "M-s M-s") 'sr-speedbar-toggle)

;; diff
(setq ediff-split-window-function 'split-window-horizontally)

(custom-set-faces
 ;; custom-set-faces was added by Custom.
 ;; If you edit it by hand, you could mess it up, so be careful.
 ;; Your init file should contain only one such instance.
 ;; If there is more than one, they won't work right.
 '(font-lock-function-name-face ((t (:foreground "blue" :weight bold))))
 '(which-func ((t (:foreground "magenta")))))
(put 'erase-buffer 'disabled nil)
