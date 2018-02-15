#
# This makefile uses `mk-configure` macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On Ubuntu
# Linux, install `mk-configure` and BSD make with `apt-get install
# mk-configure`.
#
# On other POSIX-compliant platforms like Mac OS X and *BSD, you can
# install bmake and mk-configure from pkgsrc.org. bmake is in the pkgsrc
# bootstrap kit. The package for mk-configure is in devel/.
#

LIB=rvu

STRIPFLAG=
CC?=clang
CFLAGS+=-std=c11
#COPTS+=-O3
COPTS+=-g
CFLAGS+=-Wmissing-prototypes
CPPFLAGS+=-I$(.CURDIR)/../../include
SRCS=io.c
WARNS=4

.include <mkc.lib.mk>
