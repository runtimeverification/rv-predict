TET_ROOT = ../../../..
LIBDIR	 = $(TET_ROOT)/lib/tet3
INCDIR	 = $(TET_ROOT)/inc/tet3
CC	 = cc
CFLAGS	 = -I$(INCDIR) -D_POSIX_SOURCE

INSTALL:	chmod-tc

chmod-tc:	chmod-tc.c $(INCDIR)/tet_api.h
		$(CC) $(CFLAGS) -o chmod-tc chmod-tc.c $(LIBDIR)/tcm.o $(LIBDIR)/libapi.a
		-rm -f chmod-tc.o

CLEAN:		clean

clean:
		rm -f chmod-tc chmod-tc.o bld_res

lint:
		lint $(CFLAGS) chmod-tc.c -ltcm
