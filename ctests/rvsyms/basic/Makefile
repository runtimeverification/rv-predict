NOMAN=
MKINSTALL=no
.include "../../../compat.mk"

PATH:=$(.OBJDIR)/../../bin:$(PATH)
.export PATH

CC?=clang
PROG=basic
SRCS=basic.c foo.c frame.c
WARNS=5
COPTS+=-g -O0
LDADD+=-L$(DWARF_LDDIR) -ldwarf -L$(ELF_LDDIR) -lelf
CPPFLAGS+=-I$(.CURDIR)/../../../include

.include "test.mk"

.include <mkc.prog.mk>
