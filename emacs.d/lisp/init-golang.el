(require-package 'go-mode)

;; go get github.com/rogpeppe/godef

;; a function for jumping to the file's imports (go-goto-imports - C-c C-f i)
;; a function for adding imports, including tab completion (go-import-add, bound to C-c C-a)
;; a function for removing or commenting unused imports (go-remove-unused-imports)

;; godef-describe (C-c C-d) to describe expressions
;; godef-jump (C-c C-j) and godef-jump-other-window (C-x 4 C-c C-j) to jump to declarations

(add-hook 'before-save-hook #' gofmt-before-save)
(provide 'init-golang)
