NOMAN=

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
RVP_TRACE_FILE:=/dev/null
RVP_TRACE_ONLY:=yes

.export PATH
.export RVP_TRACE_FILE
.export RVP_TRACE_ONLY

PROG=errno

PREDICT_CC?=rvpc
CC=$(PREDICT_CC)
CPPFLAGS+=-I$(.CURDIR)/../../../include
SRCS.errno=errno.c
WARNS=4
STRIPFLAG=

CFLAGS+=-Wno-unused
COPTS+=-O3 -g -O0
LDADD+=-pthread

.PHONY: test_output

test_output: $(PROG)
	@$(.OBJDIR)/$(PROG) || echo "return value is $$?"

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
