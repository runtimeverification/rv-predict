NOMAN=

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)

.export PATH
.export RVP_TRACE_FILE
.export RVP_TRACE_ONLY

PROG=cancellation

CC?=rvpc
CPPFLAGS+=-I$(.CURDIR)/../../../include
SRCS.cancellation=cancellation.c
WARNS=4
STRIPFLAG=

COPTS+=-O3 -g -O0
LDADD+=-pthread

.PHONY: test_output

test_output:
	@timeout --signal INT 1s $(.OBJDIR)/cancellation 2>&1 | \
	    grep 'No races found' || true

CLEANFILES+=test.trace

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
