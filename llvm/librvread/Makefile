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

LIB=rvread

.PATH: $(.CURDIR)/../ngrt

STRIPFLAG=
CC?=clang
CFLAGS+=-std=c11
#COPTS+=-O3
COPTS+=-g
CFLAGS+=-Wmissing-prototypes
CPPFLAGS+=-Wuninitialized -I${.CURDIR}/../ngrt -I${.CURDIR}/../../include
CPPFLAGS+=-D_POSIX_C_SOURCE=200112L	# for write(2) on QNX
CPPFLAGS+=-fPIC
CPPFLAGS+=-m64
SRCS=reader.c
WARNS=4

.include <mkc.lib.mk>
