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

TARGET_CC?=clang
CC?=$(TARGET_CC)

.include <mkc.init.mk>

.include "../../cross-libdir.mk"

LIB?=rvprt
STRIPFLAG=
INCS=rvpredict_intr.h
CFLAGS+=-std=c11
CFLAGS+=-pedantic
CFLAGS+=-fPIC
#COPTS+=-O3
COPTS+=-g
CFLAGS+=-Wall -Wmissing-prototypes
CPPFLAGS+=-I$(.CURDIR)/../../include
CPPFLAGS+=-D_POSIX_C_SOURCE=200112L	# for sigismember, sigaction on QNX,
					# POSIX threads and scheduling,
					# pthread_kill(3)
CPPFLAGS+=-D_GNU_SOURCE	# for <dlfcn.h> constant RTLD_NEXT

SRCS+=access.c const.c deltops.c
SRCS+=fork.c func.c
SRCS+=lock.c notimpl.c opinit.c
.if $(OS:Uunknown) != "QNX"
SRCS+=offline_analysis.c online_analysis.c
.else
SRCS+=stubs.c
.endif
SRCS+=register.c relay.c ring.c rmw.c rvpsignal.c
SRCS+=serialize.c sigutil.c specific.c supervise.c
SRCS+=thread.c trace.c
SRCS+=intr.c io.c
SRCS+=stru.c
WARNS=4
LINKS=${LIBDIR}/librvprt.a ${LIBDIR}/librvprt64.a
LIBC_A!=$(TARGET_CC) -print-file-name=libc.a

.if 1
SRCS+=str.c
SRCS+=istr.c
.else
SRCS+=xstr.o
str.o: str.c

xstr.o.tmp: str.o
	$(NM) -go $(LIBC_A) 2> /dev/null | \
	awk '$$(NF - 1)!="U" {s = $$NF; sub(/^/, "__rvpredict_internal_", s); print $$NF, s }' | sort -u > $(.OBJDIR)/internal-libc-redefines
	$(NM) -go $(LIBC_A) 2> /dev/null | \
	awk '$$(NF - 1)=="i" {s = $$NF; sub(/^/, "__rvpredict_internal_", s); print s }' | sort -u > $(.OBJDIR)/internal-libc-localize
	$(OBJCOPY) --redefine-syms=$(.OBJDIR)/internal-libc-redefines $(LIBC_A) $(.OBJDIR)/internal-libc.a
	$(LD) -i -o $(.TARGET) $(.ALLSRC) $(.OBJDIR)/internal-libc.a

xstr.o: xstr.o.tmp
	$(OBJCOPY) --localize-symbols=$(.OBJDIR)/internal-libc-localize $(.ALLSRC) $(.TARGET)
#	$(OBJCOPY) -L __rvpredict_internal_memcpy -L __rvpredict_internal_memmove -L __rvpredict_internal_memset $(.ALLSRC) $(.TARGET)

CLEANFILES+=internal-libc-localize internal-libc-redefines internal-libc.a intern str.d str.o xstr.o.tmp
.endif

.include <mkc.lib.mk>
