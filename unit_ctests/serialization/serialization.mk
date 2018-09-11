PROG=main

.PATH: ${.CURDIR}/../../../llvm/librvu ${.CURDIR}/../../../llvm/ngrt ${.CURDIR}/..

TARGET_CC?=clang
CC?=$(TARGET_CC)
CPPFLAGS+=-Wuninitialized -I${.CURDIR}/../../../llvm/ngrt
CPPFLAGS+=-I${.CURDIR}/../../../include
CPPFLAGS+=-I${.CURDIR}/../../../llvm/librvu
CPPFLAGS+=-D_POSIX_C_SOURCE=200112L
.if $(OS:Uunknown) == "QNX"
CPPFLAGS+=-D_QNX_SOURCE
.else
CPPFLAGS+=-D_BSD_SOURCE -D_DEFAULT_SOURCE
.endif
WARNS=4
SRCS=main.c
SRCS+=deltops.c serialize.c
SRCS+=test_mutex.c
SRCS+=test_opinit.c
SRCS+=io.c
STRIPFLAG=

COPTS+=-g -O0
