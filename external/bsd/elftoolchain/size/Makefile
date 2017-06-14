# $Id: Makefile 2043 2011-10-23 14:49:16Z jkoshy $

.include <bsd.own.mk>

TOP=	$(.CURDIR)/..

PROG=   size
WARNS?= 6
LDADD=  -L$(LIBELFTC) -lelftc -L$(LIBELF) -lelf
DPADD=	${LIBELFTC} ${LIBELF}

.include "${TOP}/mk/elftoolchain.prog.mk"

