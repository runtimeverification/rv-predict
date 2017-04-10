NOMAN=
.include "../compat.mk"

CC=clang
PROG=rvsyms
SRCS=rvsyms.c strstk.c
WARNS=4
COPTS+=-g -O3
LDADD+=$(DWARF_LDDIR) -ldwarf $(ELF_LDDIR) -lelf
CPPFLAGS+=-D_GNU_SOURCE		# for asprintf(3)
CPPFLAGS+=-I$(.CURDIR)/../../llvm/ngrt/
CPPFLAGS+=-I$(.CURDIR)/../walker

.PATH: $(.CURDIR)/../walker

.include <mkc.prog.mk>
