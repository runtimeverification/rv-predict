#
#
#
PROG=memcpyx

NOMAN=
MKINSTALL=no

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
RVP_TRACE_FILE:=$(.OBJDIR)/test.trace 
RVP_TRACE_ONLY:=yes

.export PATH

CLEANFILES+=test.trace

CC?=rvpc
CPPFLAGS+=-I$(.CURDIR)/../../../include
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
	$(.OBJDIR)/$(PROG) 2>&1 > /dev/null && false || true

.include <mkc.minitest.mk>


.include <mkc.prog.mk>
