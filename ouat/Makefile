OS!=uname -s

.if $(OS) == "Linux"
SUBDIR+=elftoolchain .WAIT
.endif

SUBDIR+=rvsyms .WAIT test

#SUBDIR=stackstruct walker

.include <mkc.subdir.mk>
