NOMAN=
#
# This makefile uses mk-configure macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On a
# POSIX-compliant platforms like Linux, Mac OS X, or *BSD, I recommend
# installing bmake and mk-configure from pkgsrc.org. bmake is in the
# pkgsrc bootstrap kit. The package for mk-configure is in devel/.
#

PATH:=$(.OBJDIR)/../../../toolset/bin:$(PATH)
RVP_TRACE_FILE:=/dev/null
RVP_TRACE_ONLY:=yes
RVP_TRACE_SIZE_LIMIT:=-10

.export PATH
.export RVP_TRACE_FILE
.export RVP_TRACE_ONLY
.export RVP_TRACE_SIZE_LIMIT

PROG=negative



.if $(MKCOVERAGE1:U"no")=="yes"
# I do not why this isn't passed down from ~/rv-predict/Makefile
#    I don't know what LDFLAGS should be - this is overkill, probably
LDFLAGS+=--coverage -fprofile-instr-generate -fcoverage-mapping
.endif


CC?=rvpc
CPPFLAGS+=-I$(.CURDIR)/../../../../include
SRCS.$(PROG)=$(PROG).c
WARNS=4
STRIPFLAG=

COPTS+=-O3 -g -O0
LDADD+=-pthread

.PHONY: test_output

test_output:
	@$(.OBJDIR)/$(PROG) 2>&1 | grep -v "^$(PROG): RVP_TRACE_SIZE_LIMIT ($(RVP_TRACE_SIZE_LIMIT)) is negative" || true

CLEANFILES+=core

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
