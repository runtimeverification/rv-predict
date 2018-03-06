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

LIB?=rvprt

STRIPFLAG=
INCS=intr_exports.h
CC?=clang
CFLAGS+=-std=c11
#COPTS+=-O3
COPTS+=-g
CFLAGS+=-Wmissing-prototypes
CPPFLAGS+=-I$(.CURDIR)/../../include
CPPFLAGS+=-D_GNU_SOURCE	# for <dlfcn.h> constant RTLD_NEXT
SRCS=access.c const.c deltops.c func.c lock.c notimpl.c
SRCS+=opinit.c
SRCS+=register.c relay.c ring.c rmw.c rvpsignal.c serialize.c sigutil.c
SRCS+=xstr.o
SRCS+=supervise.c
SRCS+=thread.c trace.c
SRCS+=intr.c
WARNS=4
LINKS=${LIBDIR}/librvprt.a ${LIBDIR}/librvprt64.a
LIBC_A!=$(CC) -print-file-name=libc.a

str.o: str.c

xstr.o: str.o
	$(NM) -go $(LIBC_A) 2> /dev/null | \
	awk '$$(NF - 1)!="U" {s = $$NF; sub(/^/, "__rvpredict_internal_", s); print $$NF, s}' | sort -u > $(.OBJDIR)/internal-libc-redefines
	$(OBJCOPY) --redefine-syms=$(.OBJDIR)/internal-libc-redefines $(LIBC_A) $(.OBJDIR)/internal-libc.a
	$(LD) -i -o $(.TARGET) $(.ALLSRC) $(.OBJDIR)/internal-libc.a

.include <mkc.lib.mk>
