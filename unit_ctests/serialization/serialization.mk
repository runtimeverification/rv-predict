PROG=main

.PATH: ${.CURDIR}/../../../llvm/librvu ${.CURDIR}/../../../llvm/ngrt ${.CURDIR}/..

CC?=clang
CPPFLAGS+=-Wuninitialized -I${.CURDIR}/../../../llvm/ngrt
CPPFLAGS+=-I${.CURDIR}/../../../include
CPPFLAGS+=-I${.CURDIR}/../../../llvm/librvu
CPPFLAGS+=-D_POSIX_C_SOURCE=200112L
CPPFLAGS+=-D_QNX_SOURCE
WARNS=4
SRCS=main.c
SRCS+=deltops.c serialize.c
SRCS+=test_opinit.c
SRCS+=io.c
STRIPFLAG=

COPTS+=-g -O0
