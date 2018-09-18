.include "../dirs.mk"

.include <mkc.init.mk>

MAKEFLAGS+=	PREDICT_CC=qrvpc
MAKEFLAGS+=	TARGET_CC=qclang
MAKEFLAGS+=	HOST_CC=clang
OS=QNX
.export OS

.include "../common.mk"

SUBDIR=mkobjdirs .WAIT
SUBDIR+=lib

.if !make(tags)
SUBDIR+=${CROSS_SUBDIR}
.endif # !make(tags)

CLEANDIRS+=cross llvm

.include <mkc.subdir.mk>
