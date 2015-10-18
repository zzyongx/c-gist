(require-package 'malabar-mode)
(add-hook 'after-init-hook
	  (lambda ()
	    (message "active-malabar-mode")
	    (activate-malabar-mode)))
(add-hook 'malabar-java-mode-hook 'flycheck-mode)
			     
(provide 'init-malabar)
