#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#
.include <bsd.own.mk>

PROGS=	dot-product lpcq memcpy spcq square-sum

CC=clang
CFLAGS+=-std=c11
#CPPFLAGS+=-D_POSIX_C_SOURCE=2
CPPFLAGS+=-D_POSIX_C_SOURCE=199309L
CPPFLAGS.spcq+=-D__STDC_NO_ATOMICS__
WARNS=4

SRCS.dot-product=dot-product.c
SRCS.square-sum=square-sum.c
SRCS.memcpy=memcpy.c
SRCS.lpcq=lpcq.c lpcq_main.c
SRCS.spcq=spcq.c spcq_main.c

.include <bsd.prog.mk>
