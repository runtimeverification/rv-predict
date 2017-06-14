# $Id: Makefile 2044 2011-10-23 14:52:59Z jkoshy $

.include <bsd.own.mk>

TOP=	$(.CURDIR)/..

PROG=   strings
WARNS?= 6
DPADD=	${LIBELFTC} ${LIBELF}
LDADD=  -L$(LIBELFTC) -lelftc -L$(LIBELF) -lelf

.include "${TOP}/mk/elftoolchain.prog.mk"

