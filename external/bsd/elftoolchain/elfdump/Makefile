# $Id: Makefile 2289 2011-12-04 07:11:47Z jkoshy $

.include <bsd.own.mk>

TOP=	$(.CURDIR)/..

PROG=	elfdump
WARNS?=	6

DPADD=	${LIBELFTC} ${LIBELF}
LDADD=	-L$(LIBELFTC) -lelftc -L$(LIBELF) -lelf

.include "${TOP}/mk/elftoolchain.prog.mk"
