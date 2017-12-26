(require-package 'imenu-list)

(setq imenu-list-focus-after-activation t)
(setq imenu-list-position 'left)
(setq imenu-list-auto-resize t)
(setq imenu-list-size 0.40)
;; (imenu-list-minor-mode)

;; <enter>: goto entry under cursor, or toggle case-folding.
;; <space>: display entry under cursor, but *Ilist* buffer remains current
;; f: expand/collapse subtree
;; n: next line
;; f: previous line
;; g: manually refresh entries
;; q: quit

(global-set-key (kbd "C-c l") 'imenu-list-minor-mode)

(provide 'init-imenu)
