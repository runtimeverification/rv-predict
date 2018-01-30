NOMAN=
MKINSTALL=no
.include "../../../compat.mk"

PATH:=$(.OBJDIR)/../../bin:$(PATH)
.export PATH
CC?=rvpc
PROG=on_signal
SRCS=on_signal.c
WARNS=5
CPPFLAGS+=-I$(.CURDIR)/../../../include

.include "test.mk"

.include <mkc.prog.mk>
