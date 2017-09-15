.include <mkc.init.mk>

BUILD_DIR_OPT=-build-dir $(.OBJDIR)

SRCDIR=$(.CURDIR)/../rv-error/src/main/ocaml/

cleandir distclean:
	cd $(SRCDIR) && $(MAKE) uninstall clean

# TBD
tags:
	@touch $(.CURDIR)/tags

install: all

$(TARGETS:Ncleandir:Ndistclean:Nfilelist:Ninstalldirs:Ntags):
	cd $(SRCDIR) && unset DESTDIR && \
	$(MAKE) $(.TARGET)
