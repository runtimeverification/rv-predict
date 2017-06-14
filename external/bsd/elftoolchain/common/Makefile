# $Id: Makefile 3022 2014-04-17 18:05:58Z jkoshy $

.include <bsd.own.mk>

TOP=	$(.CURDIR)/..

INCS=		elfdefinitions.h
INCSDIR=	/usr/include

.PHONY:	all clean clobber depend obj

all depend obj:

clean clobber:
	rm -f ${CLEANFILES}

cleandepend:
	rm -f .depend

.include "${TOP}/mk/elftoolchain.inc.mk"
