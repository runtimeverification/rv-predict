__TOP=${.CURDIR}/../..
.if $(CROSS_SRCTOP:Unone) == "none"
CROSS_SRCTOP:=${__TOP:tA}
CROSS_OBJTOP:=${MAKEOBJDIR:U${.OBJDIR}}
.export CROSS_SRCTOP
.export CROSS_OBJTOP
.endif	# CROSS_SRCTOP unset

.include "${.PARSEDIR}/../cross-libdir.mk"
