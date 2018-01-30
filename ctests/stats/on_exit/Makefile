NOMAN=
MKINSTALL=no
.include "../../../compat.mk"

PATH:=$(.OBJDIR)/../../bin:$(PATH)
.export PATH
CC?=rvpc
PROG=on_exit
SRCS=on_exit.c
WARNS=5
CPPFLAGS+=-I$(.CURDIR)/../../../include

.include "test.mk"

.include <mkc.prog.mk>
