NOMAN=
MKINSTALL=no
.include "../../../compat.mk"

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
.export PATH


.if $(MKCOVERAGE1:U"no")=="yes"
# I do not why this isn't passed down from ~/rv-predict/Makefile
#    I don't know what LDFLAGS should be - this is overkill, probably
LDFLAGS+=--coverage -fprofile-instr-generate -fcoverage-mapping
.endif


CC?=rvpc
LDFLAGS+=-pthread	# needed if CC is overridden with clang or gcc
PROG=on_signal
WARNS=5
CPPFLAGS+=-I$(.CURDIR)/../../../include

.include "test.mk"

.include <mkc.prog.mk>
