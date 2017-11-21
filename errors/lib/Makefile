.include <mkc.init.mk>

BUILD_DIR_OPT=-build-dir $(.OBJDIR)

SRCDIR=$(.CURDIR)/../rv-error/src/main/ocaml/

_TOPOBJDIR!=$(MAKE) -C $(.CURDIR)/../.. -V "$(.OBJDIR)"
OPAMROOT=$(_TOPOBJDIR)/.opam
.export OPAMROOT

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
