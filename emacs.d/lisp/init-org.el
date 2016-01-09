(require-package 'htmlize)
(org-babel-do-load-languages
 'org-babel-load-languages
 '((dot . t)
   (perl . t)
   (java . t)
   (C . t)))

(setq org-src-fontify-natively t)
(setq org-startup-folded 'showall)
(setq
 org-link-abbrev-alist
 '(("blog" . "http://zzyongx.github.io/blogs/%s.html")))

(setq
 org-publish-project-alist
 '(("org"
    :base-directory "."
    :base-extension "org"
    :with-tags t
    :publishing-function org-html-publish-to-html
    :publishing-directory "../blogs")))
(setq org-export-babel-evaluate nil)
(setq org-confirm-babel-evaluate nil)

(setq org-startup-truncated nil)
	
(provide 'init-org)
