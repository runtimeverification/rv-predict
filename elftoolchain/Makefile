.include <mkc.init.mk>

SRCDIR=$(.CURDIR)/../external/bsd/elftoolchain

cleandir distclean:
	cd $(SRCDIR) && WITH_TESTS=no MAKEFLAGS= $(MAKE) clean

#DOLLAR:=\$$$$
#FROM_DIR!=cd $(SRCDIR) && WITH_TESTS=no MAKEFLAGS= $(MAKE) -V .CURDIR
#MAKEOBJDIR:=$${DOLLAR}{.CURDIR:C,^$(FROM_DIR),$(.OBJDIR),}

#.export DOLLAR MAKEOBJDIR

# TBD
tags:
	@touch $(.CURDIR)/tags

install: all

$(TARGETS:Ncleandir:Ndistclean:Ninstall:Nfilelist:Ninstalldirs:Nuninstall:Ntags):
	cd $(SRCDIR) && unset DESTDIR && \
	LD_LIBRARY_PATH=$(HOME)/pkg/lib:/usr/lib/x86_64-linux-gnu \
	C_INCLUDE_PATH=$(HOME)/pkg/include \
	WITH_TESTS=no MAKEFLAGS= $(MAKE) MKPIC=no $(.TARGET)

