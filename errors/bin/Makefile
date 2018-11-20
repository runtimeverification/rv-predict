.include <mkc.init.mk>

FILESMODE=0755
FILES=rvp-error rvp-html-report

CLEANFILES+=$(FILES)

BUILD_DIR_OPT=-build-dir $(.OBJDIR)

SRCDIR=$(.CURDIR)/../rv-error/src/main/ocaml

.if $(RVPMAKE_DIR:Unone) == none
.error "RVPMAKE_DIR must be set.  Run `rvpmake`."
.endif

cleandir distclean:
	cd $(SRCDIR) && $(MAKE) clean

# TBD
tags:
	@touch $(.CURDIR)/tags

test:
	@echo "Nothing to do for $(.TARGET)"

rvp-error: $(SRCDIR)/rv-error
	@cp $(.ALLSRC) $(.OBJDIR)/rvp-error

rvp-html-report: .PHONY
	@cp $(.CURDIR)/../html-report/rv-html-report $(.OBJDIR)/rvp-html-report

obj:

$(TARGETS:Ncleandir:Ndistclean:Ninstall:Nfilelist:Ninstalldirs:Nobj:Nuninstall:Ntags:Nall:Ndepend:Ntest):
	cd $(SRCDIR) && unset DESTDIR && \
	$(MAKE) $(.TARGET)

$(SRCDIR)/rv-error: .PHONY
	cd $(SRCDIR) && unset DESTDIR && \
	$(MAKE) native

.include <mkc.files.mk>
