OS!=uname -s

PROJECTNAME=rv-predict-c

SUBPRJ=doc ldscript scripts:cross cross:llvm errors:reports

.if $(OS) == "Linux" || $(OS) == "QNX"
SUBPRJ+=elftoolchain:rvsyms
.else
SUBPRJ+=rvsyms
.endif

.if $(MKJAR:Uyes) == "yes"
SUBPRJ+=maven
.endif

.if $(ONLY_TEST_DEPENDENCIES:Uno) == "no"
SUBPRJ+=cross:ctests llvm:ctests maven:ctests rvsyms:ctests scripts:ctests reports:ctests ldscript:ctests doc:ctests examples:ctests
SUBPRJ+=unit_ctests
.endif

.include <mkc.subprj.mk>
