SUBDIR=scripts .WAIT llvm ouat

.if $(MKJAR:Uyes) == "yes"
SUBDIR+=maven
.endif

.include <mkc.subdir.mk>
