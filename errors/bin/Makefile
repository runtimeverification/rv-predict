.include <mkc.init.mk>

FILESMODE=0755
FILES=rvp-error

CLEANFILES+=rvp-error

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

test uninstall:
	@echo "Nothing to do for $(.TARGET)"

rvp-error: $(SRCDIR)/rv-error
	@cp $(.ALLSRC) $(.OBJDIR)/rvp-error

$(TARGETS:Ncleandir:Ndistclean:Ninstall:Nfilelist:Ninstalldirs:Nuninstall:Ntags:Nall:Ndepend:Ntest):
	cd $(SRCDIR) && unset DESTDIR && \
	$(MAKE) $(.TARGET)

$(SRCDIR)/rv-error: .PHONY
	cd $(SRCDIR) && unset DESTDIR && \
	$(MAKE) native

.include <mkc.files.mk>
