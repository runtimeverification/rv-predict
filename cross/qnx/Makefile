.include <mkc.init.mk>

.include "crossdirs.mk"

__TOP=${.CURDIR}/../..
CROSS_SRCTOP:=${__TOP:tA}
CROSS_OBJTOP:=${.OBJDIR}
MAKEFLAGS+=	PREDICT_CC=qrvpc
MAKEFLAGS+=	TARGET_CC=qclang
MAKEFLAGS+=	HOST_CC=clang
MAKEFLAGS+=	OS=QNX
MAKEOBJDIR='$${.CURDIR:C,^${CROSS_SRCTOP},${CROSS_OBJTOP},}'
MAKEFLAGS+=	MAKEOBJDIR='$${.CURDIR:C,^${CROSS_SRCTOP},${CROSS_OBJTOP},}'
#MKOBJDIRS=yes
.export CROSS_SRCTOP
.export CROSS_OBJTOP
.export MKOBJDIRS
SUBDIR=dirshack .WAIT
SUBDIR+=lib
.if !make(tags)
SUBDIR+=${CROSS_SUBDIR}
.endif # !make(tags)

.include <mkc.subdir.mk>
