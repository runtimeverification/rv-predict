NOMAN=

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
RVP_TRACE_FILE:=$(.OBJDIR)/test.trace 
RVP_TRACE_ONLY:=yes

.export PATH
.export RVP_TRACE_FILE
.export RVP_TRACE_ONLY

PROG=forkfreeze

PREDICT_CC?=rvpc
CC=$(PREDICT_CC)
CPPFLAGS+=-I$(.CURDIR)/../../../include
SRCS.forkfreeze=forkfreeze.c
WARNS=4
STRIPFLAG=

COPTS+=-O3 -g -O0
LDADD+=-pthread

.PHONY: test_output

test.trace:
	@$(.OBJDIR)/forkfreeze > /dev/null

test_output: test.trace
	@rvpdump -t symbol-friendly $(RVP_TRACE_FILE) | rvpsymbolize $(.OBJDIR)/forkfreeze | $(.CURDIR)/../../normalize-humanized-trace

CLEANFILES+=test.trace

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
