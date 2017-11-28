.include <mkc.init.mk>

SRCDIR=$(.CURDIR)/../external/bsd/elftoolchain

#
# I set variables such as MAKEFLAGS= in the environment when children
# of `make` (sub-makes) also need to see the value.  I pass the WITH_*
# variables on the command line because only the top-level `make`
# needs to see it.  I could probably pass the WITH_* variables in the
# environment, too.
#
cleandir distclean:
	cd $(SRCDIR) && \
	MAKEFLAGS="-j $(.MAKE.JOBS:U1)" $(MAKE) WITH_DOCUMENTATION=no WITH_TESTS=no clean

#DOLLAR:=\$$$$
#FROM_DIR!=cd $(SRCDIR) && MAKEFLAGS= $(MAKE) WITH_TESTS=no -V .CURDIR
#MAKEOBJDIR:=$${DOLLAR}{.CURDIR:C,^$(FROM_DIR),$(.OBJDIR),}

#.export DOLLAR MAKEOBJDIR

# TBD
tags:
	@touch $(.CURDIR)/tags

test uninstall:

install: all

$(TARGETS:Ncleandir:Ndistclean:Ninstall:Nfilelist:Ninstalldirs:Nuninstall:Ntags:Ntest):
	cd $(SRCDIR) && unset DESTDIR && \
	LD_LIBRARY_PATH=$(HOME)/pkg/lib:/usr/lib/x86_64-linux-gnu \
	C_INCLUDE_PATH=$(HOME)/pkg/include \
	MAKEFLAGS="-j $(.MAKE.JOBS:U1)" $(MAKE) WITH_DOCUMENTATION=no WITH_TESTS=no MKPIC=no \
	$(.TARGET)

