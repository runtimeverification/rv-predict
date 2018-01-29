#
# This makefile should be built with `mkcmake`.  Install it with
# `apt-get install mk-configure`.
#
.-include "../mkbin.mk"

FILESDIR=$(DATADIR)/examples/rv-predict-c/c11
INCSDIR=$(FILESDIR)
INCS=lpcq.h nbcompat.h signals.h spcq.h
FILES=Makefile
FILES+=README.lpcq README.signal
FILES+=$(SRCS.dot-product) $(SRCS.double-checked-locking)
FILES+=$(SRCS.spinning-loop) $(SRCS.square-sum) $(SRCS.memcpy)
FILES+=$(SRCS.simple-state-machine)
FILES+=$(SRCS.simplerace) $(SRCS.lpcq) $(SRCS.spcq) $(SRCS.signal)
FILES+=$(SRCS.slpcq)
FILESMODE=0644

.if $(MKBIN:Uno) == "yes"
PROGS=	dot-product double-checked-locking lpcq memcpy signal
PROGS+=	simple-state-machine simplerace slpcq spcq spinning-loop square-sum
#PROGS+=	stolz-queue
.endif

CC=clang
CFLAGS+=-std=c11
CFLAGS+=-Wmissing-prototypes
CPPFLAGS+=-D_POSIX_C_SOURCE=199506L
#CPPFLAGS+=-Dlpcq_atomic=_Atomic
#CPPFLAGS.spcq+=-D__STDC_NO_ATOMICS__
WARNS=4

SRCS.dot-product=dot-product.c
SRCS.double-checked-locking=double-checked-locking.c
SRCS.spinning-loop=spinning-loop.c
SRCS.square-sum=square-sum.c
SRCS.simple-state-machine=simple-state-machine.c
SRCS.memcpy=memcpy.c
SRCS.simplerace=simplerace.c
SRCS.lpcq=lpcq.c lpcq_main.c signals.c
SRCS.slpcq=slpcq.c slpcq_main.c signals.c
SRCS.spcq=spcq.c spcq_main.c
SRCS.signal=signal.c signals.c
SRCS.stolz-queue=stolz-queue.c

.include "../rvp.mk"
.include <mkc.prog.mk>
