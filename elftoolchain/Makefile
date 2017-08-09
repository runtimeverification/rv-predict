.include <mkc.init.mk>

SRCDIR=$(.CURDIR)/../external/bsd/elftoolchain

cleandir distclean:
	cd $(SRCDIR) && \
	MAKEFLAGS= $(MAKE) WITH_DOCUMENTATION=no WITH_TESTS=no clean

#DOLLAR:=\$$$$
#FROM_DIR!=cd $(SRCDIR) && MAKEFLAGS= $(MAKE) WITH_TESTS=no -V .CURDIR
#MAKEOBJDIR:=$${DOLLAR}{.CURDIR:C,^$(FROM_DIR),$(.OBJDIR),}

#.export DOLLAR MAKEOBJDIR

# TBD
tags:
	@touch $(.CURDIR)/tags

uninstall:
	@echo "Nothing to do for $(.TARGET)"

install: all

$(TARGETS:Ncleandir:Ndistclean:Ninstall:Nfilelist:Ninstalldirs:Nuninstall:Ntags):
	cd $(SRCDIR) && unset DESTDIR && \
	LD_LIBRARY_PATH=$(HOME)/pkg/lib:/usr/lib/x86_64-linux-gnu \
	C_INCLUDE_PATH=$(HOME)/pkg/include \
	MAKEFLAGS= $(MAKE) WITH_DOCUMENTATION=no WITH_TESTS=no MKPIC=no \
	$(.TARGET)

