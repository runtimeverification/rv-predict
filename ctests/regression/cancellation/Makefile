NOMAN=
#
# This makefile uses mk-configure macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On a
# POSIX-compliant platforms like Linux, Mac OS X, or *BSD, I recommend
# installing bmake and mk-configure from pkgsrc.org. bmake is in the
# pkgsrc bootstrap kit. The package for mk-configure is in devel/.
#

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
