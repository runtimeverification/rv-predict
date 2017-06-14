#
#	SCCS: @(#)shared.mk	1.6 (03/03/31)
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1996 X/Open Company Limited
#
# All rights reserved.  No part of this source code may be reproduced,
# stored in a retrieval system, or transmitted, in any form or by any
# means, electronic, mechanical, photocopying, recording or otherwise,
# except as stated in the end-user licence agreement, without the prior
# permission of the copyright owners.
# A copy of the end-user licence agreement is contained in the file
# Licence which accompanies this distribution.
# 
# X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
# the UK and other countries.
#
# ************************************************************************
#
# SCCS:   	@(#)shared.mk	1.6 03/03/31
# NAME:		shared.mk
# PRODUCT:	TETware
# AUTHOR:	Geoff Clare, UniSoft Ltd.
# DATE CREATED:	October 1996
#
# DESCRIPTION:
#	make include file, shared between tcm, tcmthr, tcmshlib and tcmthrshlib
# 
# MODIFICATIONS:
#
#	Andrew Dingwall, UniSoft Ltd., August 1998
#	Added support for shared libraries.
#
#	Andrew Dingwall, UniSoft Ltd., July 1999
#	Moved TCM code out of the API library.
#	Now, tcm.o and friends are built using ld -r on UNIX systems
#	and a shellscript that fakes the same thing on Win32 systems.
#
# ************************************************************************

# generic object files
# (The ordering of these files is important on Win32 systems, in order for
# the UGLY HACK in w32_ld-r.ksh to work; in particular, dynlink.obj must
# be the first input file named on the w32_ld-r command line.)
TCM_OFILES_GN = dynlink$O tcm_main$O tcmfuncs$O ictp$O ckversion$O
TCMCHILD_OFILES_GN = dynlink$O tcmc_main$O child$O tcmfuncs$O ckversion$O
TCMREM_OFILES_GN = dynlink$O tcmr_main$O child$O tcmfuncs$O ckversion$O

# the transport-specific object files are defined in ts.mk which is included
# in the makefile before this file

# object file components
# (See note about ordering above.)
# C API
TCM_M_OFILES = $(TCM_OFILES_GN) $(TCM_OFILES_TS)
TCM_OFILES = $(TCM_M_OFILES) main$O
TCMC_M_OFILES = $(TCMCHILD_OFILES_GN) $(TCMCHILD_OFILES_TS)
TCMCHILD_OFILES = $(TCMC_M_OFILES) main_ch$O
TCMR_M_OFILES = $(TCMREM_OFILES_GN) $(TCMREM_OFILES_TS)
TCMREM_OFILES = $(TCMR_M_OFILES) main_rem$O

# C++ API
# (See note about ordering above.)
CTCM_OFILES = $(TCM_M_OFILES) Cmain$O
CTCMCHILD_OFILES = $(TCMC_M_OFILES) Cmain_ch$O


# linking
tcm$O: $(TCM_OFILES)
	$(LD_R) -o $@ $(TCM_OFILES)

tcmchild$O: $(TCMCHILD_OFILES)
	$(LD_R) -o $@ $(TCMCHILD_OFILES)

tcmrem$O: $(TCMREM_OFILES)
	$(LD_R) -o $@ $(TCMREM_OFILES)

tcm_m$O: $(TCM_M_OFILES)
	$(LD_R) -o $@ $(TCM_M_OFILES)

tcmc_m$O: $(TCMC_M_OFILES)
	$(LD_R) -o $@ $(TCMC_M_OFILES)

tcmr_m$O: $(TCMR_M_OFILES)
	$(LD_R) -o $@ $(TCMR_M_OFILES)

Ctcm$O: $(CTCM_OFILES)
	$(LD_R) -o $@ $(CTCM_OFILES)

Ctcmchild$O: $(CTCMCHILD_OFILES)
	$(LD_R) -o $@ $(CTCMCHILD_OFILES)


# compilations using TET_CFLAGS

child$O: $(TCMSRC)child.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)child.c

ckversion$O: $(TCMSRC)ckversion.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)ckversion.c

dynlink$O: $(TCMSRC)dynlink.c dynlink_gen_made
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)dynlink.c

# note that the dependences in the following rule are incomplete,
# but it's the best we can do;
# the higher level makefile makes api*shlib before tcm*shlib, so it doesn't
# usually matter
dynlink_gen_made:
	@set -x;							\
	if test -n "$(APISHLIBSRC)";					\
	then								\
		cd $(APISHLIBSRC);					\
		$(MAKE) dynlink.gen;					\
	fi
	touch $@

fake$O: $(TCMSRC)fake.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)fake.c

ictp$O: $(TCMSRC)ictp.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)ictp.c

main$O: $(TCMSRC)main.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)main.c

main_ch$O: $(TCMSRC)main_ch.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)main_ch.c

main_rem$O: $(TCMSRC)main_rem.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)main_rem.c

tcmc_main$O: $(TCMSRC)tcmc_main.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)tcmc_main.c

tcmfuncs$O: $(TCMSRC)tcmfuncs.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)tcmfuncs.c

tcmr_main$O: $(TCMSRC)tcmr_main.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)tcmr_main.c

tcm_bs$O: $(TCMSRC)tcm_bs.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(TCMSRC)tcm_bs.c


# compilations using DTET_CFLAGS

# tcm_main.c does the TCM signal trapping so it needs all the signal names to
# be visible
tcm_main$O: $(TCMSRC)tcm_main.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(TCMSRC)tcm_main.c

tcm_in$O: $(TCMSRC)tcm_in.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(TCMSRC)tcm_in.c

tcm_xt$O: $(TCMSRC)tcm_xt.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(TCMSRC)tcm_xt.c


# C++ compilations

Cmain$O: $(TCMSRC)main.c
	rm -f Cmain.$(C_SUFFIX)
	cp $(TCMSRC)main.c Cmain.$(C_SUFFIX)
	$(C_PLUS) $(TET_CFLAGS) -c Cmain.$(C_SUFFIX)
	@set -x;						\
	case `uname -s` in					\
	Windows_*)						\
		;;						\
	*)							\
		rm -f Cmain.$(C_SUFFIX)				\
		;;						\
	esac

Cmain_ch$O: $(TCMSRC)main_ch.c
	rm -f Cmain_ch.$(C_SUFFIX)
	cp $(TCMSRC)main_ch.c Cmain_ch.$(C_SUFFIX)
	$(C_PLUS) $(TET_CFLAGS) -c Cmain_ch.$(C_SUFFIX)
	@set -x;						\
	case `uname -s` in					\
	Windows_*)						\
		;;						\
	*)							\
		rm -f Cmain_ch.$(C_SUFFIX)			\
		;;						\
	esac


# dependencies
child$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h $(INC)/apilib.h \
	$(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h $(INC)/sigsafe.h \
	$(INC)/synreq.h $(INC)/tslib.h $(TCMSRC)tcmfuncs.h $(TCMSRC)tcmhdrs.h

ckversion$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h \
	$(INC)/apilib.h $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h \
	$(INC)/sigsafe.h $(INC)/synreq.h $(INC)/tslib.h ../apilib/version.c \
	$(TCMSRC)tcmfuncs.h $(TCMSRC)tcmhdrs.h

dynlink$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h $(INC)/ptab.h \
	$(INC)/server.h $(TCMSRC)tcmfuncs.h 

fake$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h $(INC)/apilib.h \
	$(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h $(INC)/sigsafe.h \
	$(INC)/synreq.h $(INC)/tslib.h $(TCMSRC)tcmfuncs.h $(TCMSRC)tcmhdrs.h

ictp$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h $(INC)/apilib.h \
	$(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h $(INC)/sigsafe.h \
	$(INC)/synreq.h $(INC)/tslib.h $(TCMSRC)tcmfuncs.h $(TCMSRC)tcmhdrs.h

main$O Cmain$O: $(DINC)/tet_api.h

main_ch$O Cmain_ch$O: $(DINC)/tet_api.h

main_rem$O: $(DINC)/tet_api.h

tcm_bs$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h $(INC)/apilib.h \
	$(INC)/avmsg.h $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/server_bs.h \
	$(INC)/servlib.h $(INC)/sigsafe.h $(INC)/synreq.h $(INC)/tslib.h \
	$(INC)/valmsg.h $(TCMSRC)tcmfuncs.h $(TCMSRC)tcmhdrs.h

tcm_in$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h $(INC)/apilib.h \
	$(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h $(INC)/inetlib_in.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/server_in.h \
	$(INC)/servlib.h $(INC)/sigsafe.h $(INC)/synreq.h $(INC)/tptab_in.h \
	$(INC)/tsinfo_in.h $(INC)/tslib.h $(TCMSRC)tcmfuncs.h \
	$(TCMSRC)tcmhdrs.h

tcm_main$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h \
	$(INC)/apilib.h $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h \
	$(INC)/sigsafe.h $(INC)/synreq.h $(INC)/tslib.h $(TCMSRC)tcmfuncs.h \
	$(TCMSRC)tcmhdrs.h

tcm_xt$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h $(INC)/apilib.h \
	$(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/server.h $(INC)/server_xt.h $(INC)/servlib.h \
	$(INC)/sigsafe.h $(INC)/synreq.h $(INC)/tptab_xt.h $(INC)/tsinfo_xt.h \
	$(INC)/tslib.h $(INC)/xtilib_xt.h $(TCMSRC)tcmfuncs.h \
	$(TCMSRC)tcmhdrs.h

tcmc_main$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h \
	$(INC)/apilib.h $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h \
	$(INC)/sigsafe.h $(INC)/synreq.h $(INC)/tslib.h $(TCMSRC)tcmfuncs.h \
	$(TCMSRC)tcmhdrs.h

tcmfuncs$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h \
	$(INC)/apilib.h $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h \
	$(INC)/sigsafe.h $(INC)/synreq.h $(INC)/tslib.h $(TCMSRC)tcmfuncs.h \
	$(TCMSRC)tcmhdrs.h

tcmr_main$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/alarm.h \
	$(INC)/apilib.h $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h \
	$(INC)/sigsafe.h $(INC)/synreq.h $(INC)/tslib.h $(TCMSRC)tcmfuncs.h \
	$(TCMSRC)tcmhdrs.h

