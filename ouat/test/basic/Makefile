NOMAN=
MKINSTALL=no

CC?=clang
PROG=basic
SRCS=basic.c foo.c frame.c
WARNS=5
DBG+=-g -O0
LDADD+=$(DWARF_LDDIR) -ldwarf $(ELF_LDDIR) -lelf
CPPFLAGS+=-I$(.CURDIR)/../../../include

.include "test.mk"

.include <mkc.prog.mk>
