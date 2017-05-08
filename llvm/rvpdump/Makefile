NOMAN=
#
# This makefile uses mk-configure macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On a
# POSIX-compliant platforms like Linux, Mac OS X, or *BSD, I recommend
# installing bmake and mk-configure from pkgsrc.org. bmake is in the
# pkgsrc bootstrap kit. The package for mk-configure is in devel/.
#

PROGS=rvpdump metadatadump tracedump

CC?=clang
CPPFLAGS+=-Wuninitialized -I${.CURDIR}/../ngrt
SRCS.rvpdump=main.c reader.c
SRCS.metadatadump=metadatadump.c
SRCS.tracedump=tracedump.c
WARNS=4
STRIPFLAG=

COPTS+=-g -O3

.include <mkc.prog.mk>
