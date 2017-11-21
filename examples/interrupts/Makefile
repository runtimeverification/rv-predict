#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#
.-include "../mkbin.mk"

FILESDIR=$(DATADIR)/examples/rv-predict-c/interrupts
INCSDIR=$(FILESDIR)
INCS=lib.h
FILES=Makefile
FILES+=README
FILES+=$(SRCS.1)
FILES+=$(SRCS.2)
FILES+=$(SRCS.3)
FILESMODE=0644

.if $(MKBIN:Uno) == "yes"
PROGS=	1 2 3
.endif

CC=clang
CFLAGS+=-std=c11
CPPFLAGS+=-D_POSIX_C_SOURCE=199506L
CPPFLAGS+=-I$(.CURDIR)/../c11
#CPPFLAGS.spcq+=-D__STDC_NO_ATOMICS__
WARNS=4

SRCS.1=1.c lib.c
SRCS.2=2.c lib.c
SRCS.3=3.c lib.c

.include "../rvp.mk"

.include <mkc.prog.mk>
