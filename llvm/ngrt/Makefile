#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#
.include <bsd.own.mk>

LIB=rvprt

CC=clang
CFLAGS+=-std=c11
COPTS+=-O3
DBG+=-g
CPPFLAGS+=-D_GNU_SOURCE	# for <dlfcn.h> constant RTLD_NEXT
SRCS=access.c const.c func.c lock.c notimpl.c ring.c rmw.c signal.c sigutil.c
SRCS+=thread.c trace.c
WARNS=4

.include <bsd.lib.mk>
