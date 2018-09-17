.include "crossdirs.mk"

__TOP=${.CURDIR}/../..
CROSS_SRCTOP:=${__TOP:tA}
CROSS_OBJTOP:=${.OBJDIR}
MAKEFLAGS+=	MKOBJDIRS=auto
MAKEFLAGS+=     MAKEOBJDIR='$$(.CURDIR:C,${CROSS_SRCTOP},${CROSS_OBJTOP},)'
.export CROSS_SRCTOP
.export CROSS_OBJTOP
.info CROSS_OBJTOP=$(CROSS_OBJTOP)
.info CROSS_SRCTOP=$(CROSS_SRCTOP)
.info .OBJDIR=$(.OBJDIR)
.info MAKEOBJDIR=$(MAKEOBJDIR)
.info OS=$(OS)
.info MAKEFLAGS=$(MAKEFLAGS)

SUBDIR=dirshack .WAIT
SUBDIR+=lib

.if !make(tags)
SUBDIR+=${CROSS_SUBDIR}
.endif # !make(tags)

CLEANDIRS+=cross llvm

.include <mkc.subdir.mk>
