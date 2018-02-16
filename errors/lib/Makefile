.include <mkc.init.mk>

BUILD_DIR_OPT=-build-dir $(.OBJDIR)

SRCDIR=$(.CURDIR)/../rv-error/src/main/ocaml/

.if $(RVPMAKE_DIR:Unone) == none
.error "RVPMAKE_DIR must be set.  Run `rvpmake`."
.endif

cleandir distclean:
	cd $(SRCDIR) && $(MAKE) uninstall clean

# TBD
tags:
	@touch $(.CURDIR)/tags

test depend uninstall:

install: all

$(TARGETS:Ncleandir:Ndistclean:Nfilelist:Ninstalldirs:Ntags:Ndepend:Ntest:Nuninstall):
	cd $(SRCDIR) && unset DESTDIR && \
	$(MAKE) $(.TARGET)
