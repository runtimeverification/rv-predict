#
# This makefile should be built with `mkcmake`.  Install it with
# `apt-get install mk-configure`.
#
.-include "../mkbin.mk"

FILESDIR=$(DATADIR)/examples/rv-predict-c/interrupts
INCSDIR=$(FILESDIR)
INCS=lib.h
FILES=Makefile
FILES+=README.md
FILES+=$(SRCS.1)
FILES+=$(SRCS.2)
FILES+=$(SRCS.3)
FILES+=$(SRCS.sim)
FILESMODE=0644

.if $(MKBIN:Uno) == "yes"
PROGS=	1 2 3
PROGS+=	sim
.endif

CC=clang
CFLAGS+=-std=c11
CPPFLAGS+=-D_POSIX_C_SOURCE=200809L
CPPFLAGS+=-I$(.CURDIR)/../c11
CPPFLAGS+=-I$(HOME)/include
#CPPFLAGS.spcq+=-D__STDC_NO_ATOMICS__
WARNS=4

SRCS.sim=sim.c
SRCS.1=1.c lib.c
SRCS.2=2.c lib.c
SRCS.3=3.c lib.c

.include "../rvp.mk"

.include <mkc.prog.mk>
