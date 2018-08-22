#
#
#
PROG=memsetx

NOMAN=
MKINSTALL=no

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
RVP_TRACE_FILE:=/dev/null
RVP_TRACE_ONLY:=yes

.export PATH

CC?=rvpc
CPPFLAGS+=-I$(.CURDIR)/../../../include
WARNS=4
STRIPFLAG=

COPTS+=-O0 -g
LDADD+=-pthread

.PHONY: test_output
 
LOCAL_NORMALIZE?=cat

test_output:
	@set -e; \
	$(.OBJDIR)/$(PROG) 2>&1 > /dev/null && false || true

.include <mkc.minitest.mk>
.include <mkc.prog.mk>
