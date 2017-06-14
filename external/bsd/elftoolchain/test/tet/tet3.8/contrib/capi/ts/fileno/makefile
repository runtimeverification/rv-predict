TET_ROOT = ../../../..
LIBDIR	 = $(TET_ROOT)/lib/tet3
INCDIR	 = $(TET_ROOT)/inc/tet3
CC	 = cc
CFLAGS	 = -I$(INCDIR) -D_POSIX_SOURCE

INSTALL:	fileno-tc

fileno-tc:	fileno-t4 fileno-tc.c $(INCDIR)/tet_api.h
		$(CC) $(CFLAGS) -o fileno-tc fileno-tc.c $(LIBDIR)/tcm.o $(LIBDIR)/libapi.a
		-rm -f fileno-tc.o

fileno-t4:	fileno-t4.c $(INCDIR)/tet_api.h
		$(CC) $(CFLAGS) -o fileno-t4 fileno-t4.c $(LIBDIR)/tcmchild.o $(LIBDIR)/libapi.a
		-rm -f fileno-t4.o

CLEAN:		clean
clean:
		rm -f fileno-tc fileno-tc.o fileno-t4 fileno-t4.o bld_res

lint:
		lint $(CFLAGS) fileno-tc.c -ltcm
		lint $(CFLAGS) fileno-t4.c -ltcmc
