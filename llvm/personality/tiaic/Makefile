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

.include "../../../cross-libdir.mk"

LIB?=rvpi_tiaic
INCS=rvpredict_intr_tiaic.h

.PATH: $(.CURDIR)/../../ngrt

CC?=clang
CFLAGS+=-std=c11
CFLAGS+=-pedantic
CFLAGS+=-Wmissing-prototypes
#COPTS+=-O3
COPTS+=-g
CFLAGS+=-Wmissing-prototypes
CPPFLAGS+=-I$(.CURDIR)/../../ngrt -I$(.CURDIR)/../../../include
CPPFLAGS+=-D_GNU_SOURCE	# for <dlfcn.h> constant RTLD_NEXT
CPPFLAGS+=-D_POSIX_C_SOURCE=200112L
SRCS+=tiaic_intr.c tiaic_reg.c
LINKS=${LIBDIR}/librvpi_tiaic.a ${LIBDIR}/librvpi_tiaic64.a

.include <mkc.lib.mk>
