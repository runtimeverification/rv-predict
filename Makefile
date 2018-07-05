OS!=uname -s

PROJECTNAME=rv-predict-c

SUBPRJ=doc examples ldscript scripts:llvm errors:reports unit_ctests

.if $(OS) == "Linux"
SUBPRJ+=elftoolchain:rvsyms
.else
SUBPRJ+=rvsyms
.endif

.if $(MKJAR:Uyes) == "yes"
SUBPRJ+=maven
.endif

#
# Code coverage
RVPRoot=~/rv-predict

.if $(MKCOVERAGE1:U"no")=="yes"
#       each .c gets a .gcno at compile time and .gcda at execution
#       --coverage includes -fprofile-arcs -ftest-coverage
#       -g includes debug info (as per https://llvm.org/docs/CommandGuide/llvm-cov.html )
CFLAGS+=--coverage -g
#    I don't know what LDFLAGS should be - this is overkill, probably
LDFLAGS+=--coverage -fprofile-instr-generate -fcoverage-mapping
.endif

.if $(MKCOVERAGE:U"no")=="yes"
#      Generates default.profraw when program runs
CFLAGS+=-fprofile-instr-generate -fcoverage-mapping 
LDFLAGS+=-fprofile-instr-generate -fcoverage-mapping`
.endif
#
# end coverage

#
.if $(ONLY_TEST_DEPENDENCIES:Uno) == "no"
SUBPRJ+=llvm:ctests maven:ctests rvsyms:ctests scripts:ctests reports:ctests ldscript:ctests doc:ctests examples:ctests
.endif

.include <mkc.subprj.mk>
