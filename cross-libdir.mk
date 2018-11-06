#
# TBD move this to cross/qnx/Makefile
#
.if $(OS:Uunknown) == QNX
LIBDIR=$(PREFIX)/lib/qnx
.elif defined(PACKAGE_LIBDIR) && $(USE_PACKAGE_LIBDIR:Uno) == yes
LIBDIR=$(PACKAGE_LIBDIR)
.endif

.export LIBDIR	# exporting this is important!

MAKEFLAGS+= CROSS_SRCTOP=$(CROSS_SRCTOP) CROSS_OBJTOP=$(CROSS_OBJTOP) MAKEOBJDIR='$$(.CURDIR:C,$(CROSS_SRCTOP),$(CROSS_OBJTOP),)'

.if $(DEBUG:Uno) == yes
.info enter .CURDIR=$(.CURDIR)
.info enter .OBJDIR=$(.OBJDIR)
.info enter MAKEOBJDIR=$(MAKEOBJDIR)
.info enter MKOBJDIRS=$(MKOBJDIRS)
.info enter OS=$(OS)
.info enter MAKEFLAGS=$(MAKEFLAGS)
.info enter LIBDIR=$(LIBDIR)
.endif
