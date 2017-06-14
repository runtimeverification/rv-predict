
# Default directory locations
TET_ROOT =	../..
INSTLIB =	$(TET_ROOT)/lib/sun_lib
SUNLIB = sun_lib.a

CC =	cc
RM =	rm -f
LINT =	lint

# Library building commands: if $(AR) adds symbol tables itself, or the
# system has ranlib, set LORDER=echo and TSORT=cat.  If ranlib is not
# required, set RANLIB=echo.

AR =	ar cr
RANLIB =	echo
LORDER =	echo
TSORT =	cat

COPTS =	
DEFINES =	$(OS)
INCS =		-I../inc -I$(TET_ROOT)/inc/tet3
CFLOCAL =
CFLAGS =	$(CFLOCAL) $(INCS) $(DEFINES) $(COPTS)

LINTFLAGS =	$(INCS) $(DEFINES) -n
#LINTLIBS =	-lposix1

CFILES =	sun_func.c
OFILES =	sun_func.o
#


$(INSTLIB)/INSTALLED:	$(OFILES) $(SUNLIB)
		@mkdir -p $(INSTLIB)
		cp $(OFILES) $(SUNLIB) $(INSTLIB)
		touch $(INSTLIB)/INSTALLED

all:		$(OFILES) $(SUNLIB)

$(OFILES): sun_tet.h

$(SUNLIB):	$(OFILES)
		$(AR) $@ `$(LORDER) $(OFILES) | $(TSORT)`
		$(RANLIB) $@

$(OFILES):	$(TET_ROOT)/inc/tet3/tet_api.h

FORCE:		CLOBBER all

LINT:
		$(LINT) $(LINTFLAGS) $(CFILES) $(LINTLIBS)

LINTLIB:	llib-ltcm.ln llib-ltcmc.ln

CLEAN:
		$(RM) $(OFILES) Makefile.bak  $(SUNLIB)

CLOBBER:	CLEAN
		$(RM) $(INSTLIB)/$(OFILES) $(INSTLIB)/$(SUNLIB)

clean:		CLEAN

clobber:	CLOBBER
