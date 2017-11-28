OS!=uname -s

PROJECTNAME=rv-predict-c

SUBPRJ=doc examples ldscript scripts:llvm errors:reports

.if $(OS) == "Linux"
SUBPRJ+=elftoolchain:rvsyms
.else
SUBPRJ+=rvsyms
.endif

.if $(MKJAR:Uyes) == "yes"
SUBPRJ+=maven
.endif

SUBPRJ+=llvm:ctests maven:ctests rvsyms:ctests scripts:ctests

.include <mkc.subprj.mk>
