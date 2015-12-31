(require-package 'htmlize)
(org-babel-do-load-languages
 'org-babel-load-languages
 '((dot . t)
   (perl . t)
   (java . t)
   (C . t)))

(setq org-src-fontify-natively t)
(setq org-startup-folded 'showall)
(setq org-publish-project-alist
      '(("org"
 	 :base-directory "."
	 :base-extension "org"
	 :with-tags t
	 :publishing-function org-html-publish-to-html
	 :publishing-directory "../tmp")
	("html"
	 :base-directory "../tmp"
	 :base-extension "html"
	 :auto-sitemap t
	 :sitemap-filename "index.org"
	 :sitemap-title "sitemap"
	 :sitemap-sort-files 'anti-chronologically
	 :publishing-function org-publish-attachment
	 :publishing-directory "../blogs")
	("sitemap"
	 :base-directory "../blogs"
	 :base-extension "org"
	 :publishing-function org-html-publish-to-html
	 :publishing-directory "../blogs")))
(setq org-export-babel-evaluate nil)
(setq org-confirm-babel-evaluate nil)

(setq org-startup-truncated nil)
	
(provide 'init-org)
