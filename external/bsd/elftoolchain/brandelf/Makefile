# $Id: Makefile 2066 2011-10-26 15:40:28Z jkoshy $

.include <bsd.own.mk>

TOP=	$(.CURDIR)/..

PROG=	brandelf
WARNS?=	6
LDADD=	-L$(LIBELFTC) -lelftc -L$(LIBELF) -lelf

.include "${TOP}/mk/elftoolchain.prog.mk"
