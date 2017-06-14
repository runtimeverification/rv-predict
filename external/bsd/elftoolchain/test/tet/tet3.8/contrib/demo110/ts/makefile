# Check that the definitions below are correct for your system

LIBDIR	= $(TET_ROOT)/lib/tet3
INCDIR	= $(TET_ROOT)/inc/tet3
CC	= cc
# GNU CC
#CC	= gcc

CFLAGS	= -I$(INCDIR)
# for SunOS4.1
#CFLAGS =  -I$(INCDIR) -D_POSIX_SOURCE -DNSIG=32 -DEXIT_SUCCESS=0 -DEXIT_FAILURE=1 -DNULL=0

# when using Distributed TETware on systems such as SVR4 and Solaris
# SYSLIBS = -lsocket -lnsl
SYSLIBS =

all:	tc1 tc2 tc3
	chmod a+x tc1 tc2 tc3

tc1:	tc1.c $(INCDIR)/tet_api.h
	$(CC) $(CFLAGS) -o tc1 tc1.c $(LIBDIR)/tcm.o $(LIBDIR)/libapi.a $(SYSLIBS)

tc2:	tc2.c $(INCDIR)/tet_api.h
	$(CC) $(CFLAGS) -o tc2 tc2.c $(LIBDIR)/tcm.o $(LIBDIR)/libapi.a $(SYSLIBS)

tc3:	tc3.c $(INCDIR)/tet_api.h
	$(CC) $(CFLAGS) -o tc3 tc3.c $(LIBDIR)/tcm.o $(LIBDIR)/libapi.a $(SYSLIBS)

