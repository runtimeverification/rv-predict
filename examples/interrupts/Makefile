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
FILES+=$(SRCS.4)
FILES+=$(SRCS.5)
FILES+=$(SRCS.6)
FILES+=$(SRCS.p1)
FILES+=$(SRCS.p2)
FILES+=$(SRCS.p3)
FILES+=$(SRCS.sim)
FILESMODE=0644

.if $(MKBIN:Uno) == "yes"
PROGS=	p1 p2 p3 1 2 3 4 5 6
PROGS+=	sim
.endif

CC=clang
CFLAGS+=--intr-personality=basic
LDFLAGS+=--intr-personality=basic
CFLAGS+=-std=c11
CPPFLAGS+=-D_POSIX_C_SOURCE=200809L
CPPFLAGS+=-I$(.CURDIR)/../c11
CPPFLAGS+=-I$(HOME)/include
#CPPFLAGS.spcq+=-D__STDC_NO_ATOMICS__
WARNS=4

SRCS.p1=p1.c lib.c
SRCS.p2=p2.c lib.c
SRCS.p3=p3.c lib.c
SRCS.1=1.c lib.c
SRCS.2=2.c lib.c
SRCS.3=3.c lib.c
SRCS.4=4.c lib.c
SRCS.5=5.c lib.c
SRCS.6=6.c lib.c
SRCS.sim=sim.c

.include "../rvp.mk"

.include <mkc.prog.mk>
.include <mkc.subdir.mk>
