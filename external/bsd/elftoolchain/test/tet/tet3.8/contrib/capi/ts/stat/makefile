TET_ROOT =  ../../../..
LIBDIR	 = $(TET_ROOT)/lib/tet3
INCDIR	 = $(TET_ROOT)/inc/tet3
CC	 = cc
CFLAGS	 = -I$(INCDIR) -D_POSIX_SOURCE

INSTALL:	stat-tc

stat-tc:	stat-tc.c $(INCDIR)/tet_api.h
		$(CC) $(CFLAGS) -o stat-tc stat-tc.c $(LIBDIR)/tcm.o $(LIBDIR)/libapi.a
		-rm -f stat-tc.o

CLEAN:		clean

clean:
		rm -f stat-tc stat-tc.o bld_res 

lint:
		lint $(CFLAGS) stat-tc.c -ltcm
