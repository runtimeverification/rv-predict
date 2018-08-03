#
#
#
PROG=memmovex

NOMAN=
MKINSTALL=no

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
RVP_TRACE_FILE:=$(.OBJDIR)/test.trace 
RVP_TRACE_ONLY:=yes

CTEST_dir=$(.CURDIR)/../..

.export PATH


CLEANFILES+=test.trace

CC?=rvpc
CPPFLAGS+=-I$(CTEST_dir)/../include
WARNS=4
STRIPFLAG=


COPTS+=-O0 -g
LDADD+=-pthread

.PHONY: test_output
 
test.trace: $(PROG)
	@$(.OBJDIR)/$(PROG) > /dev/null

LOCAL_NORMALIZE?=cat

test_output:
	@set -e; \
	$(PROG) 2>&1 > /dev/null && false || true

.include <mkc.minitest.mk>


.include <mkc.prog.mk>
