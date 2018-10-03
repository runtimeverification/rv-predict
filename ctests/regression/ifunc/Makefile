NOMAN=

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
RVP_TRACE_FILE:=/dev/null
RVP_TRACE_ONLY:=yes

.export PATH
.export RVP_TRACE_FILE
.export RVP_TRACE_ONLY

PROG=ifunc

PREDICT_CC?=rvpc
CC=$(PREDICT_CC)
CPPFLAGS+=-I$(.CURDIR)/../../../include
SRCS.ifunc=ifunc.c
WARNS=4
STRIPFLAG=

CFLAGS+=-Wno-unused
COPTS+=-O3 -g -O0
LDADD+=-pthread

.PHONY: test_output

test.trace:
	@$(.OBJDIR)/ifunc > /dev/null

test_output: $(PROG)
	@$(.OBJDIR)/$(PROG) 2>&1

CLEANFILES+=test.trace

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
