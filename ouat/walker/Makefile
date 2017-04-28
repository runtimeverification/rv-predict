NOMAN=
MKINSTALL=no
.include "../compat.mk"

CC=clang
PROG=walker
SRCS=walker.c
WARNS=4
COPTS+=-g -O3
LDADD+=$(DWARF_LDDIR) -ldwarf $(ELF_LDDIR) -lelf
CPPFLAGS+=-D_GNU_SOURCE		# for asprintf(3)
CPPFLAGS+=-I../../llvm/ngrt/

.include <mkc.prog.mk>
