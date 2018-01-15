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

.if $(ONLY_TEST_DEPENDENCIES:Uno) == "no"
SUBPRJ+=llvm:ctests maven:ctests rvsyms:ctests scripts:ctests
.endif

.include <mkc.subprj.mk>
