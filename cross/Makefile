MAKEOBJDIR='$${.CURDIR:C,^${CROSS_SRCTOP},${CROSS_OBJTOP},}'
MAKEFLAGS+=	MKOBJDIRS=auto
MAKEFLAGS+=	MAKEOBJDIR='$${.CURDIR:C,^${CROSS_SRCTOP},${CROSS_OBJTOP},}'
#MKOBJDIRS=auto
#.export MKOBJDIRS
.export MAKEOBJDIR

.if !make(tags)
SUBDIR=linux qnx
.endif # !make(tags)

.include <mkc.subdir.mk>
