.include <mkc.init.mk>

FILESMODE=0755
FILES=rv-error

CLEANFILES+=rv-error

BUILD_DIR_OPT=-build-dir $(.OBJDIR)

SRCDIR=$(.CURDIR)/../rv-error/src/main/ocaml

cleandir distclean:
	cd $(SRCDIR) && $(MAKE) clean

# TBD
tags:
	@touch $(.CURDIR)/tags

uninstall:
	@echo "Nothing to do for $(.TARGET)"

rv-error: $(SRCDIR)/rv-error
	@cp $(.ALLSRC) $(.OBJDIR)/.

$(TARGETS:Ncleandir:Ndistclean:Ninstall:Nfilelist:Ninstalldirs:Nuninstall:Ntags:Nall:Ndepend):
	cd $(SRCDIR) && unset DESTDIR && \
	$(MAKE) $(.TARGET)

$(SRCDIR)/rv-error: .PHONY
	cd $(SRCDIR) && unset DESTDIR && \
	$(MAKE) native

#rv-error: $(.CURDIR)/../errors/rv-error/src/main/ocaml/rv_error_native.ml
#	ocamlfind opt -g $(.ALLSRC) -o $(.OBJDIR)/rv-error -package atdgen -package rv_error -linkpkg -package unix

.include <mkc.files.mk>
