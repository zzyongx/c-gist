(require-package 'htmlize)
(org-babel-do-load-languages
 'org-babel-load-languages
 '((dot . t)
   (perl . t)
   (java . t)
   (C . t)))

;; (require 'org)
(eval-after-load "org"
  '(progn
     (setcar (nthcdr 2 org-emphasis-regexp-components) " \t\n,")
     (custom-set-variables `(org-emphasis-alist ',org-emphasis-alist))))

;; This one is for the beginning char
(setcar org-emphasis-regexp-components " \t('\"{[:alpha:]")
;; This one is for the ending char.
(setcar (nthcdr 1 org-emphasis-regexp-components) "[:alpha:]- \t.,:!?;'\")}\\")
(org-set-emph-re 'org-emphasis-regexp-components org-emphasis-regexp-components)

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
