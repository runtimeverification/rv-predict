PROG=main

.PATH: ${.CURDIR}/../../../llvm/ngrt ${.CURDIR}/..

CC?=clang
CPPFLAGS+=-Wuninitialized -I${.CURDIR}/../../../llvm/ngrt
CPPFLAGS+=-I${.CURDIR}/../../../include
WARNS=4
SRCS=main.c
SRCS+=deltops.c serialize.c
SRCS+=test_opinit.c
STRIPFLAG=

COPTS+=-g -O0
