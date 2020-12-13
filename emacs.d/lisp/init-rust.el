(require-package 'rust-mode)

(add-hook 'rust-mode-hook
          (lambda () (setq indent-tabs-mode nil)))
(setq rust-format-on-save t)
(setq rust-indent-offset 2)

(provide 'init-rust)
