.include "../../../../cross-libdir.mk"

LIB=rvpmissing_posix
INCSDIR=${PREFIX}/include/qnx

STRIPFLAG=
TARGET_CC?=clang
CC?=$(TARGET_CC)
CFLAGS+=$(TARGET_CFLAGS)
CFLAGS+=-std=c11
#COPTS+=-O3
COPTS+=-g
CFLAGS+=-Wmissing-prototypes
CPPFLAGS+=-I$(.CURDIR)/../../include
CPPFLAGS+=-D_POSIX_C_SOURCE=200112L

INCS=rvpredict_missing_posix.h
SRCS=missing_posix.c
WARNS=4

.include <mkc.lib.mk>
