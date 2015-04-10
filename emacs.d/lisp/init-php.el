(require-package 'php-mode)
(add-hook 'php-mode-hook
          '(lambda()
             (setq-default tab-width 2 indent-tabs-mode t)
             (setq c-basic-offset 2)
            ))
(provide 'init-php)
