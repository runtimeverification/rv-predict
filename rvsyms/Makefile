NOMAN=
.include "../compat.mk"

CC=clang
PROG=rvsyms
SRCS=rvsyms.c strstk.c
WARNS=4
COPTS+=-g -O3
LDADD+=-L$(DWARF_LDDIR) -ldwarf -L$(ELF_LDDIR) -lelf
CPPFLAGS+=-D_GNU_SOURCE		# for asprintf(3)
CPPFLAGS+=-I$(.CURDIR)/../include

.include <mkc.prog.mk>
