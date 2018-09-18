__TOP=${.CURDIR}/../..
.if $(CROSS_SRCTOP:Unone) == "none"
CROSS_SRCTOP:=${__TOP:tA}
CROSS_OBJTOP:=${MAKEOBJDIR:U${.OBJDIR}}
.export CROSS_SRCTOP
.export CROSS_OBJTOP
.endif	# CROSS_SRCTOP unset

.if 0
MAKEFLAGS+=	MKOBJDIRS=auto
MAKEFLAGS+=     MAKEOBJDIR='$$(.CURDIR:C,${CROSS_SRCTOP},${CROSS_OBJTOP},)'

.if $(DEBUG:Uno) == yes
.info CROSS_OBJTOP=$(CROSS_OBJTOP)
.info CROSS_SRCTOP=$(CROSS_SRCTOP)
.info .OBJDIR=$(.OBJDIR)
.info MAKEOBJDIR=$(MAKEOBJDIR)
.info OS=$(OS)
.info MAKEFLAGS=$(MAKEFLAGS)
.endif
.endif # 0

.include "${.PARSEDIR}/../cross-libdir.mk"
