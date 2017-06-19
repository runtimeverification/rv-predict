#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#
.-include "../mkbin.mk"

FILESDIR=$(DATADIR)/examples/rv-predict-c/c11
INCSDIR=$(FILESDIR)
INCS=lpcq.h nbcompat.h signals.h spcq.h
FILES=Makefile
FILES=README.lpcq README.raceless-signal
FILES+=$(SRCS.dot-product) $(SRCS.square-sum) $(SRCS.memcpy)
FILES+=$(SRCS.simplerace) $(SRCS.lpcq) $(SRCS.spcq) $(SRCS.raceless-signal)
FILESMODE=0644

.if $(MKBIN:Uno) == "yes"
PROGS=	dot-product lpcq memcpy raceless-signal simplerace spcq square-sum
#PROGS+=	stolz-queue
.endif

CC=clang
CFLAGS+=-std=c11
CPPFLAGS+=-D_POSIX_C_SOURCE=199506L
#CPPFLAGS.spcq+=-D__STDC_NO_ATOMICS__
WARNS=4

SRCS.dot-product=dot-product.c
SRCS.square-sum=square-sum.c
SRCS.memcpy=memcpy.c
SRCS.simplerace=simplerace.c
SRCS.lpcq=lpcq.c lpcq_main.c signals.c
SRCS.spcq=spcq.c spcq_main.c
SRCS.raceless-signal=raceless-signal.c signals.c
SRCS.stolz-queue=stolz-queue.c

.include "../rvp.mk"
.include <mkc.prog.mk>
