#
# This makefile uses mk-configure macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On a
# POSIX-compliant platforms like Linux, Mac OS X, or *BSD, I recommend
# installing bmake and mk-configure from pkgsrc.org. bmake is in the
# pkgsrc bootstrap kit. The package for mk-configure is in devel/.
#

LIB?=rvprt

CC=clang
CFLAGS+=-std=c11
COPTS+=-O3 -O0
DBG+=-g
CPPFLAGS+=-Wuninitialized
CPPFLAGS+=-D_GNU_SOURCE	# for <dlfcn.h> constant RTLD_NEXT
SRCS=access.c const.c func.c lock.c notimpl.c
SRCS+=relay.c ring.c rmw.c rvpsignal.c sigutil.c
SRCS+=thread.c trace.c
WARNS=4

.include <mkc.lib.mk>
