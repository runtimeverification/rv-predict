SUBDIR=mkobjdirs .WAIT
SUBDIR+=lib

__SRCTOP=$(.CURDIR)/../..

CROSS_SRCTOP=$(__SRCTOP:tA)
CROSS_OBJTOP=$(.OBJDIR)

MAKEFLAGS+= CROSS_SRCTOP=$(CROSS_SRCTOP) CROSS_OBJTOP=$(CROSS_OBJTOP) MAKEOBJDIR='$$(.CURDIR:C,$(CROSS_SRCTOP),$(CROSS_OBJTOP),)'

.include "../dirs.mk"

MAKEFLAGS+=	PREDICT_CC=qrvpc
MAKEFLAGS+=	TARGET_CC=qclang
MAKEFLAGS+=	HOST_CC=clang
OS=QNX
.export OS

.if !make(tags)
SUBDIR+=${CROSS_SUBDIR}
.endif # !make(tags)

CLEANDIRS+=cross llvm

.include <mkc.subdir.mk>
