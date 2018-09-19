#
# TBD move this to cross/qnx/Makefile
#
.if $(OS:Uunknown) == QNX
LIBDIR=$(PREFIX)/lib/qnx
.export LIBDIR	# exporting this is important!
.endif

.if 1
MAKEFLAGS+= CROSS_SRCTOP=$(CROSS_SRCTOP) CROSS_OBJTOP=$(CROSS_OBJTOP) MAKEOBJDIR='$$(.CURDIR:C,$(CROSS_SRCTOP),$(CROSS_OBJTOP),)'
.endif

.if $(DEBUG:Uno) == yes
.info enter .CURDIR=$(.CURDIR)
.info enter .OBJDIR=$(.OBJDIR)
.info enter MAKEOBJDIR=$(MAKEOBJDIR)
.info enter MKOBJDIRS=$(MKOBJDIRS)
.info enter OS=$(OS)
.info enter MAKEFLAGS=$(MAKEFLAGS)
.info enter LIBDIR=$(LIBDIR)
.endif
