#
# This makefile should be built with `mkcmake`.  Install it with
# `apt-get install mk-configure`.
#
.-include "../mkbin.mk"

FILESDIR=$(DATADIR)/examples/rv-predict-c/78k0
INCSDIR=$(FILESDIR)
INCS=78k0_predict.h
FILES=Makefile
FILES+=README.md
FILES+=$(SRCS.78k0)
FILES+=$(SRCS.lolo)
FILES+=$(SRCS.hilo)
FILESMODE=0644

.if $(MKBIN:Uno) == "yes"
PROGS+=	78k0
PROGS+=	lolo
PROGS+=	hilo
.endif

LDFLAGS+=--intr-personality=78k0
CFLAGS+=--intr-personality=78k0
CFLAGS+=-std=c11
CPPFLAGS+=-D_POSIX_C_SOURCE=200809L
CPPFLAGS+=-I$(.CURDIR)/../c11
CPPFLAGS+=-I$(HOME)/include
CPPFLAGS+=-include 78k0_predict.h
WARNS=4

SRCS.78k0=78k0.c
SRCS.lolo=lolo.c
SRCS.hilo=hilo.c

.include "../rvp.mk"

.include <mkc.prog.mk>
