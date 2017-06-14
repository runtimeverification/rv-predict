# $Id: Makefile 2076 2011-10-27 03:50:33Z jkoshy $

.include <bsd.own.mk>

TOP=	$(.CURDIR)/..

PROG=	readelf
SRCS=	readelf.c

WARNS?=	6

DPADD=	${LIBDWARF} ${LIBELFTC} ${LIBELF}
LDADD=	-L$(LIBDWARF) -ldwarf -L$(LIBELFTC) -lelftc -L$(LIBELF) -lelf

MAN1=	readelf.1

.include "${TOP}/mk/elftoolchain.prog.mk"
