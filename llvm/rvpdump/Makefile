#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#
.include <bsd.own.mk>

PROG=rvpdump

CPPFLAGS+=-I../ngrt
SRCS=main.c reader.c
WARNS=5

DBG+=-O0 -g

.include <bsd.prog.mk>
