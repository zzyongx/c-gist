(require-package 'lua-mode)
(setq lua-indent-level 2)
(add-hook 'lua-mode-hook
          '(lambda()
             (setq indent-tabs-mode nil)
             (setq-default tab-width 2 indent-tabs-mode t)
             (setq c-basic-offset 2)
            ))
(provide 'init-lua)
