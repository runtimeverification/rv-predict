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

PROG=vptr1

PREDICT_CXX?=rvpc++
CXX=$(PREDICT_CXX)
CPPFLAGS+=-I$(.CURDIR)/../../../include
SRCS=vptr1.cc
WARNS=4
STRIPFLAG=

#COPTS+=-mllvm -print-after-all
#COPTS+=-mllvm -print-before-all
COPTS+=-O1
LDADD+=-pthread

.PHONY: test_output

test_output:
	@$(.OBJDIR)/vptr1 2>&1

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
