OS!=uname -s

PROJECTNAME=rv-predict-c

SUBPRJ=doc examples scripts:llvm reports

.if $(OS) == "Linux"
SUBPRJ+=elftoolchain:rvsyms
.else
SUBPRJ+=rvsyms
.endif

.if $(MKJAR:Uyes) == "yes"
SUBPRJ+=maven
.endif

.include <mkc.subprj.mk>
