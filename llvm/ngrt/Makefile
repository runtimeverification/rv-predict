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
INCS=rvpredict_intr.h
CC?=clang
CFLAGS+=-std=c11
CFLAGS+=-pedantic
#COPTS+=-O3
COPTS+=-g
CFLAGS+=-Wall -Wmissing-prototypes
CPPFLAGS+=-I$(.CURDIR)/../../include
CPPFLAGS+=-D_GNU_SOURCE	# for <dlfcn.h> constant RTLD_NEXT

#
# Code coverage

.if $(MKCOVERAGE1:U"no")=="yes"
#       each .c gets a .gcno at compile time and .gcda at execution
#       --coverage includes -fprofile-arcs -ftest-coverage
#       -g includes debug info (as per https://llvm.org/docs/CommandGuide/llvm-cov.html )
CFLAGS+=--coverage -g
.endif

.if $(MKCOVERAGE:U"no")=="yes"
#      Generates default.profraw when program runs
CFLAGS+=-fprofile-instr-generate -fcoverage-mapping 
.endif
#
SRCS+=access.c const.c deltops.c func.c lock.c notimpl.c
SRCS+=opinit.c
SRCS+=register.c relay.c ring.c rmw.c rvpsignal.c serialize.c sigutil.c
SRCS+=supervise.c
SRCS+=thread.c trace.c
SRCS+=intr.c
SRCS+=io.c
WARNS=4
LINKS=${LIBDIR}/librvprt.a ${LIBDIR}/librvprt64.a
LIBC_A!=$(CC) -print-file-name=libc.a

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
