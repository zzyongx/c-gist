(defalias 'perl-mode 'cperl-mode)
(setq-default indent-tabs-mode nil)
(setq cperl-indent-level 2
      cperl-close-paren-offset -2
      cperl-indent-parens-as-block t			
      cperl-continued-statement-offset 2
      cperl-tab-always-indent t)

(provide 'init-perl)
