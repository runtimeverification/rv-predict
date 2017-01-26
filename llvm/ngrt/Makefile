.include <bsd.own.mk>

LIB=rvprt

CC=clang
CFLAGS+=-std=c11
COPTS+=-O3
DBG+=-g
CPPFLAGS+=-D_GNU_SOURCE	# for <dlfnc.h> constant RTLD_NEXT
SRCS=access.c const.c func.c lock.c notimpl.c ring.c rmw.c thread.c trace.c
WARNS=4

.include <bsd.lib.mk>
