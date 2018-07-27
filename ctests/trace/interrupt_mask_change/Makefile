.include "../head.mk"

PROG=chgmask
.if $(CC:Urvpc) == "rvpc"
CFLAGS+="--sigsim=simple"
LDFLAGS+="--sigsim=simple"
.endif

#
# Tail portion of Makefiles in the test directory
#

CC?=rvpc
CPPFLAGS+=-I$(.CURDIR)/../../../include
WARNS=4
STRIPFLAG=

COPTS+=-O3 -g -O0
LDADD+=-pthread

.PHONY: test_output

test.trace: $(PROG)
	@$(.OBJDIR)/$(PROG) > /dev/null

test_output: test.trace
	@rvpdump -t symbol-friendly $(RVP_TRACE_FILE) | rvpsymbolize $(.OBJDIR)/$(PROG) | $(.CURDIR)/normalize | $(.CURDIR)/normalize-sigdepth

CLEANFILES+=test.trace

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
