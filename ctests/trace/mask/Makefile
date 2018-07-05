.include "../head.mk"


.if $(MKCOVERAGE1:U"no")=="yes"
# I do not why this isn't passed down from ~/rv-predict/Makefile
#    I don't know what LDFLAGS should be - this is overkill, probably
LDFLAGS+=--coverage -fprofile-instr-generate -fcoverage-mapping
.endif


PROG=mask

.include "../tail.mk"
