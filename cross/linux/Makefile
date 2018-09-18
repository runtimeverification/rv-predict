.include "../dirs.mk"

.include <mkc.init.mk>

.include "../common.mk"

SUBDIR=mkobjdirs .WAIT
SUBDIR+=lib

.if !make(tags)
SUBDIR+=${CROSS_SUBDIR}
.endif # !make(tags)

CLEANDIRS+=cross llvm

.include <mkc.subdir.mk>
