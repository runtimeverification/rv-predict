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

PROG=condvar

CC?=rvpc
CPPFLAGS+=-I$(.CURDIR)/../../../include
#LDFLAGS+=--just-print
SRCS.condvar=condvar.c
WARNS=4
STRIPFLAG=

COPTS+=-O3 -g -O0
LDADD+=-pthread -v

.PHONY: test_output

test_output: $(.OBJDIR)/condvar
	@$(.OBJDIR)/condvar 2> /dev/null | \
	    $(.CURDIR)/../../normalize-humanized-report

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
