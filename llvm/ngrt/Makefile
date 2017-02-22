#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#
.include <bsd.own.mk>

LIB=rvprt

CC=clang
CFLAGS+=-std=c11
COPTS+=-O3 -O0
DBG+=-g
CPPFLAGS+=-Wuninitialized
CPPFLAGS+=-D_GNU_SOURCE	# for <dlfcn.h> constant RTLD_NEXT
SRCS=access.c const.c func.c lock.c notimpl.c
SRCS+=relay.c ring.c rmw.c rvpsignal.c sigutil.c
SRCS+=thread.c trace.c
WARNS=5

.include <bsd.lib.mk>
