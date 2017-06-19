OS!=uname -s

SUBDIR=doc examples scripts .WAIT llvm

.if $(OS) == "Linux"
SUBDIR+=elftoolchain .WAIT
.endif

SUBDIR+=rvsyms

.if $(MKJAR:Uyes) == "yes"
SUBDIR+=maven
.endif

.include <mkc.subdir.mk>
