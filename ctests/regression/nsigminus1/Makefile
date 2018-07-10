NOMAN=

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)

RVP_TRACE_FILE:=/dev/null
RVP_TRACE_ONLY:=yes

.export PATH
.export RVP_TRACE_FILE
.export RVP_TRACE_ONLY

PROG=nsigminus1

CC?=rvpc
CPPFLAGS+=-I$(.CURDIR)/../../../include
SRCS.nsigminus1=nsigminus1.c
WARNS=4
STRIPFLAG=

COPTS+=-O3 -g -O0
LDADD+=-pthread

.PHONY: test_output

test_output: $(PROG)
	@$(.OBJDIR)/nsigminus1

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
