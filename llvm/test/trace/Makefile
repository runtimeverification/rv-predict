NOMAN=
#
# This makefile uses mk-configure macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On a
# POSIX-compliant platforms like Linux, Mac OS X, or *BSD, I recommend
# installing bmake and mk-configure from pkgsrc.org. bmake is in the
# pkgsrc bootstrap kit. The package for mk-configure is in devel/.
#

PROGS=mask

CC?=rvpc
CPPFLAGS+=-I$(.CURDIR)/../../ngrt
SRCS.mask=mask.c
WARNS=4
STRIPFLAG=

COPTS+=-O3 -g -O0

.include <mkc.prog.mk>
