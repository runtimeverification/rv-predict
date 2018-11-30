NOMAN=
#
# This makefile uses mk-configure macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On a
# POSIX-compliant platforms like Linux, Mac OS X, or *BSD, I recommend
# installing bmake and mk-configure from pkgsrc.org. bmake is in the
# pkgsrc bootstrap kit. The package for mk-configure is in devel/.
#

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
RVP_TRACE_FILE:=/dev/null
RVP_TRACE_ONLY:=yes

.export PATH
.export RVP_TRACE_FILE
.export RVP_TRACE_ONLY

PROG=setspecific

PREDICT_CC?=rvpc
CC=$(PREDICT_CC)
CPPFLAGS+=-I$(.CURDIR)/../../../include
SRCS=main.c
WARNS=4
STRIPFLAG=

COPTS+=-O3 -g -O0
LDADD+=-pthread

.PHONY: test_output

test_output:
	@$(.OBJDIR)/setspecific 2>&1

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
