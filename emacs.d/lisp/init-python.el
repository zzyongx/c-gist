(require-package 'elpy)
(require-package 'jedi)
(add-hook 'python-mode-hook
          '(lambda()
             (setq python-indent-offset 2)
            ))
(provide 'init-python)
