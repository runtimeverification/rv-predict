.PATH:${.CURDIR}/../../../llvm/ngrt

CC?=clang
CPPFLAGS+=-Wuninitialized -I${.CURDIR}/../../../llvm/ngrt
CPPFLAGS+=-I${.CURDIR}/../../../include
WARNS=4
SRCS=main.c
SRCS+=deltops.c serialize.c
STRIPFLAG=

COPTS+=-g -O3
