LIB=missingposix

STRIPFLAG=
CC?=qclang
CFLAGS+=-std=c11
#COPTS+=-O3
COPTS+=-g
CFLAGS+=-Wmissing-prototypes
CPPFLAGS+=-I$(.CURDIR)/../../include
CPPFLAGS+=-D_POSIX_C_SOURCE=200112L

INCS=missingposix.h
SRCS=missingposix.c
WARNS=4

.include <mkc.lib.mk>
