LIBDIR	= $(TET_ROOT)/lib/tet3
INCDIR	= $(TET_ROOT)/inc/tet3

# Name of C++ compiler
# USL C++ based implementations
CC	= CC

# GNU g++
#CC	= g++

CFLAGS	=  -I$(INCDIR)
# CFLAGS for SunOS41 with g++
#CFLAGS  = -I$(INCDIR) -D_POSIX_SOURCE -DNSIG=32 \
# -DEXIT_SUCCESS=0 -DEXIT_FAILURE=1 -DNULL=0 -D_WCHAR_T

LIBTCM	= $(LIBDIR)/Ctcm.o
LIBTCMCHILD	= $(LIBDIR)/Ctcmchild.o
LIBAPI	= $(LIBDIR)/libapi.a
SYSLIBS = -lsocket -lnsl

all:	tc3 tc3child
	chmod a+rx tc3 tc3child

tc3:	tc3.c tc3child $(INCDIR)/tet_api.h
	$(CC) $(CFLAGS) -o tc3 tc3.c $(LIBTCM) $(LIBAPI) $(SYSLIBS)

tc3child:	tc3child.c $(INCDIR)/tet_api.h
	$(CC) $(CFLAGS) -o tc3child tc3child.c $(LIBTCMCHILD) $(LIBAPI) $(SYSLIBS)

CLEAN:	clean


clean:
	rm -f *.o tc3 tc3child
