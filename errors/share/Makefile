FILES=datatables.min.css datatables.min.js
FILES+=error.css error-descriptions.html error.html error.js errors.js
FILES+=favicon.ico
FILES+=index.html index.js
FILES+=message.js
FILES+=jquery.min.js
FILES+=layout.html
FILES+=rv_logo.png
FILES+=sort_asc_disabled.png
FILES+=sort_asc.png
FILES+=sort_both.png
FILES+=sort_desc_disabled.png
FILES+=sort_desc.png
FILES+=sourcefile.html sourcefile.js sourcefile.css
FILES+=style.css

FILESMODE=0644

_HTML_REPORT_DIR=$(DATADIR)/rv-predict-c/html-report
_STATIC_DIR=$(_HTML_REPORT_DIR)/static
_STATIC_STYLES_DIR=$(_STATIC_DIR)/styles
_STATIC_SCRIPTS_DIR=$(_STATIC_DIR)/scripts
_STATIC_IMAGES_DIR=$(_STATIC_DIR)/images
_TEMPLATES_DIR=$(_HTML_REPORT_DIR)/templates

FILESDIR_favicon.ico=$(_STATIC_DIR)
FILESDIR_rv_logo.png=$(_STATIC_DIR)
FILESDIR_style.css=$(_STATIC_DIR)

FILESDIR_sort_both.png=$(_STATIC_IMAGES_DIR)
FILESDIR_sort_asc_disabled.png=$(_STATIC_IMAGES_DIR)
FILESDIR_sort_desc.png=$(_STATIC_IMAGES_DIR)
FILESDIR_sort_asc.png=$(_STATIC_IMAGES_DIR)
FILESDIR_sort_desc_disabled.png=$(_STATIC_IMAGES_DIR)

FILESDIR_datatables.min.js=$(_STATIC_SCRIPTS_DIR)
FILESDIR_error.js=$(_STATIC_SCRIPTS_DIR)
FILESDIR_errors.js=$(_STATIC_SCRIPTS_DIR)
FILESDIR_index.js=$(_STATIC_SCRIPTS_DIR)
FILESDIR_jquery.min.js=$(_STATIC_SCRIPTS_DIR)
FILESDIR_message.js=$(_STATIC_SCRIPTS_DIR)
FILESDIR_sourcefile.js=$(_STATIC_SCRIPTS_DIR)

FILESDIR_datatables.min.css=$(_STATIC_STYLES_DIR)
FILESDIR_error.css=$(_STATIC_STYLES_DIR)
FILESDIR_sourcefile.css=$(_STATIC_STYLES_DIR)

FILESDIR_sourcefile.html=$(_TEMPLATES_DIR)
FILESDIR_index.html=$(_TEMPLATES_DIR)
FILESDIR_error-descriptions.html=$(_TEMPLATES_DIR)
FILESDIR_error.html=$(_TEMPLATES_DIR)
FILESDIR_layout.html=$(_TEMPLATES_DIR)

$(FILES): .PHONY
.for f in $(FILES)
	@find $(.CURDIR)/../html-report -name $f | xargs -I fn cp fn $(.OBJDIR)/.
.endfor

CLEANFILES+=$(FILES)

.include <mkc.files.mk>
