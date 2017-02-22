NOMAN=
#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#
.include <bsd.own.mk>

PROGS=rvpdump metadatadump tracedump

CPPFLAGS+=-Wuninitialized -I../ngrt
SRCS.rvpdump=main.c reader.c
SRCS.metadatadump=metadatadump.c
SRCS.tracedump=tracedump.c
WARNS=5

DBG+=-O3 -g -O0

.include <bsd.prog.mk>
