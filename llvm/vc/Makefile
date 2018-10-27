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

LIB=vc

CPPFLAGS+=-D_POSIX_C_SOURCE=200809L	# for dprintf(3) and getopt(3)
#CC?=$(HOST_CC)
CPPFLAGS+=-Wuninitialized
SRCS=vc.cc
WARNS=4
STRIPFLAG=

COPTS+=-g -O3

.include <mkc.lib.mk>