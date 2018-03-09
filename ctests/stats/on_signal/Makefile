NOMAN=
MKINSTALL=no
.include "../../../compat.mk"

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
.export PATH
CC?=rvpc
PROG=on_signal
WARNS=5
CPPFLAGS+=-I$(.CURDIR)/../../../include

.include "test.mk"

.include <mkc.prog.mk>
