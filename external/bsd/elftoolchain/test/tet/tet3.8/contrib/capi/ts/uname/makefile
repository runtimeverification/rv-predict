TET_ROOT = ../../../..
LIBDIR	 = $(TET_ROOT)/lib/tet3
INCDIR	 = $(TET_ROOT)/inc/tet3
CC	 = cc
CFLAGS	 = -I$(INCDIR) -D_POSIX_SOURCE

INSTALL:	uname-tc

uname-tc:	uname-tc.c $(INCDIR)/tet_api.h
		$(CC) $(CFLAGS) -o uname-tc uname-tc.c $(LIBDIR)/tcm.o $(LIBDIR)/libapi.a
		-rm -f uname-tc.o

CLEAN:		clean

clean:
		rm -f uname-tc uname-tc.o bld_res

lint:
		lint $(CFLAGS) uname-tc.c -ltcm
