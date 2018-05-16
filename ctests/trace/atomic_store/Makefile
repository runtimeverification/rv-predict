NOMAN=
#
# This makefile uses mk-configure macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On a
# POSIX-compliant platforms like Linux, Mac OS X, or *BSD, I recommend
# installing bmake and mk-configure from pkgsrc.org. bmake is in the
# pkgsrc bootstrap kit. The package for mk-configure is in devel/.
#

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
RVP_TRACE_FILE:=$(.OBJDIR)/test.trace 
RVP_TRACE_ONLY:=yes

.export PATH
.export RVP_TRACE_FILE
.export RVP_TRACE_ONLY

PROG=atomic_store

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
	@rvpdump -t symbol-friendly $(RVP_TRACE_FILE) | rvpsymbolize $(.OBJDIR)/$(PROG) | $(.CURDIR)/../../normalize-humanized-trace

CLEANFILES+=test.trace

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
