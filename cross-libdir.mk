.if $(OS:Uunknown) == QNX
LIBDIR=$(PREFIX)/lib/qnx
.export LIBDIR	# exporting this is important!
.endif

.if $(DEBUG:Uno) == yes
.info enter CROSS_OBJTOP=$(CROSS_OBJTOP)
.info enter CROSS_SRCTOP=$(CROSS_SRCTOP)
.info enter .CURDIR=$(.CURDIR)
.info enter .OBJDIR=$(.OBJDIR)
.info enter MAKEOBJDIR=$(MAKEOBJDIR)
.info enter MKOBJDIRS=$(MKOBJDIRS)
.info enter OS=$(OS)
.info enter MAKEFLAGS=$(MAKEFLAGS)
.info enter LIBDIR=$(LIBDIR)
.endif

.if defined(CROSS_OBJTOP)
MAKEOBJDIR=$(.CURDIR:C,${CROSS_SRCTOP},${CROSS_OBJTOP},)
MAKEFLAGS+=	MKOBJDIRS=auto
MAKEFLAGS+=	MAKEOBJDIR='$$(.CURDIR:C,${CROSS_SRCTOP},${CROSS_OBJTOP},)'
.endif

.if $(DEBUG:Uno) == yes
.info exit CROSS_OBJTOP=$(CROSS_OBJTOP)
.info exit CROSS_SRCTOP=$(CROSS_SRCTOP)
.info exit .CURDIR=$(.CURDIR)
.info exit .OBJDIR=$(.OBJDIR)
.info exit MAKEOBJDIR=$(MAKEOBJDIR)
.info exit MKOBJDIRS=$(MKOBJDIRS)
.info exit OS=$(OS)
.info exit MAKEFLAGS=$(MAKEFLAGS)
.info exit LIBDIR=$(LIBDIR)
.endif

#.warning CROSS_SRCTOP=$(CROSS_SRCTOP) CROSS_OBJTOP=$(CROSS_OBJTOP) .OBJDIR=$(.OBJDIR) MAKEFLAGS=$(MAKEFLAGS)
