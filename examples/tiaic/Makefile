#
# This makefile should be built with `mkcmake`.  Install it with
# `apt-get install mk-configure`.
#
.-include "../mkbin.mk"

FILESDIR=$(DATADIR)/examples/rv-predict-c/tiaic
INCSDIR=$(FILESDIR)
INCS=tiaic_predict.h
FILES=Makefile
FILES+=README.md
FILES+=$(SRCS.simple)
FILESMODE=0644

.if $(MKBIN:Uno) == "yes"
PROGS+=	simple
.endif

LDFLAGS+=--intr-personality=tiaic
CFLAGS+=--intr-personality=tiaic
CFLAGS+=-std=c11
CPPFLAGS+=-D_POSIX_C_SOURCE=200809L
CPPFLAGS+=-I$(.CURDIR)/../c11
CPPFLAGS+=-I$(HOME)/include
CPPFLAGS+=-include tiaic_predict.h
WARNS=4

SRCS.simple=simple.c

.include "../rvp.mk"

.include <mkc.prog.mk>
