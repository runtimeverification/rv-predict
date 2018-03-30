NOMAN=

PATH:=$(.OBJDIR)/../toolset/bin:$(PATH)

.export PATH

PROG=forkfail

CC?=rvpc
CPPFLAGS+=-I$(.CURDIR)/../../include
SRCS.forkfail=forkfail.c
WARNS=4
STRIPFLAG=

COPTS+=-O3 -g -O0
LDADD+=-pthread

.PHONY: test_output

test_output:
	@timeout 10s sh $(.OBJDIR)/fork.sh 2> /dev/null

CLEANFILES+=hs_err_pid*.log $(PROG).test.out.tmp

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
