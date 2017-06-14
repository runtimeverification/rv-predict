/*
 *      SCCS:  @(#)servlib.h	1.13 (05/06/27) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 *
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

/************************************************************************

SCCS:   	@(#)servlib.h	1.13 05/06/27 TETware release 3.8
NAME:		servlib.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	declarations for extern servlib functions and data objects not
	declared in other header files

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added parameters to prototypes for tet_tctslfiles()
	and tet_tctsmfiles()

	Denis McConalogue, UniSoft Limited, September 1993
	added prototypes for sd_discon(), xd_discon(),
	sd_islogon() and xd_islogon().

	Andrew Dingwall, UniSoft Ltd., December 1993
	removed disconnect stuff

	Andrew Dingwall, UniSoft Ltd., August 1996
	added tc_mkalldirs()
	removed savedir parameter from tet_tctsmfiles()

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Geoff Clare, The Open Group, June 2005
	Added second parameter to tet_xdxropen().

************************************************************************/

/* only include this stuff in Distributed TETware */
#ifndef TET_LITE	/* -START-LITE-CUT- */

/* the server interface error numbers */
TET_IMPORT_DATA(int, tet_sderrno);
extern int tet_tcerrno;
TET_IMPORT_DATA(int, tet_xderrno);


/* extern function declarations */
extern int tet_sdislogon PROTOLIST((void));
extern int tet_sdlogoff PROTOLIST((int));
TET_IMPORT_FUNC(int, tet_sdlogon, PROTOLIST((void)));
extern char *tet_sdmsgbuf PROTOLIST((int));
extern long tet_sdsnget PROTOLIST((void));
extern int tet_sdsnrm PROTOLIST((long));
extern int tet_sdsnsys PROTOLIST((long, int *, int));
extern char *tet_sdtalk PROTOLIST((int, int));
extern void tet_si_forkdaemon PROTOLIST((void));
extern int tet_si_main PROTOLIST((int, char **, int));
extern void tet_si_serverloop PROTOLIST((void));
extern int tet_tcaccess PROTOLIST((int, char *, int));
extern int tet_tccfname PROTOLIST((int, char *));
extern int tet_tcchdir PROTOLIST((int, char *));
extern int tet_tcconfigv PROTOLIST((int, char **, int, int));
extern long tet_tcexec PROTOLIST((int, char *, char **, char *, long, long,
	int));
extern int tet_tcfclose PROTOLIST((int, int));
extern int tet_tcfopen PROTOLIST((int, char *, int));
extern int tet_tcftime PROTOLIST((int, char *, long *, long *));
extern int tet_tcfwrite PROTOLIST((int, int, char *, int));
extern int tet_tckill PROTOLIST((int, long, int));
extern int tet_tclockfile PROTOLIST((int, char *, int));
extern int tet_tclogoff PROTOLIST((int));
extern int tet_tclogon PROTOLIST((int));
extern long tet_tcmexec PROTOLIST((int, char *, char **, char *));
extern int tet_tcmkalldirs PROTOLIST((int, char *));
extern int tet_tcmkdir PROTOLIST((int, char *));
extern char *tet_tcmksdir PROTOLIST((int, char *, char *));
extern char *tet_tcmktmpdir PROTOLIST((int, char *));
extern char *tet_tcmsgbuf PROTOLIST((int, int));
extern int tet_tcputenv PROTOLIST((int, char *));
extern int tet_tcputenvv PROTOLIST((int, char **, int));
extern int tet_tcputs PROTOLIST((int, int, char *));
extern int tet_tcputsv PROTOLIST((int, int, char **, int));
extern int tet_tcrcopy PROTOLIST((int, char *, char *));
extern char **tet_tcrcvconfv PROTOLIST((int, int *, int *));
extern int tet_tcrmalldirs PROTOLIST((int, char *));
extern int tet_tcrmdir PROTOLIST((int, char *));
extern int tet_tcrsys PROTOLIST((int, int));
extern int tet_tcrxfile PROTOLIST((int, char *, char *));
extern int tet_tcsetconf PROTOLIST((int, int));
extern int tet_tcsndftype PROTOLIST((int));
extern char *tet_tcsharelock PROTOLIST((int, char *, int));
extern int tet_tcsndconfv PROTOLIST((int, char **, int));
extern int tet_tcsysname PROTOLIST((int, int *, int));
extern char *tet_tctalk PROTOLIST((int, int, int));
extern long tet_tctexec PROTOLIST((int, char *, char **, char *, long, long));
extern int tet_tctime PROTOLIST((int, long *));
extern int tet_tctslfiles PROTOLIST((int, char **, int, char *, char *));
extern int tet_tctsmfiles PROTOLIST((int, char **, int, char *));
extern long tet_tcuexec PROTOLIST((int, char *, char **, long, long));
extern int tet_tcunlink PROTOLIST((int, char *));
extern int tet_tcutime PROTOLIST((int, char *, long, long));
extern int tet_tcwait PROTOLIST((int, long, int, int *));
extern int tet_ti_tcmputenv PROTOLIST((int, long, long, int *, int));
extern int tet_xdcfname PROTOLIST((char *, char *, char *));
extern int tet_xdcodesfile PROTOLIST((char *));
extern int tet_xdfclose PROTOLIST((int));
extern int tet_xdfopen PROTOLIST((char *));
extern char *tet_xdgets PROTOLIST((int));
extern char **tet_xdgetsv PROTOLIST((int, int *, int *));
TET_IMPORT_FUNC(int, tet_xdicend, PROTOLIST((long)));
TET_IMPORT_FUNC(int, tet_xdicstart, PROTOLIST((long, int, long, int)));
extern int tet_xdislogon PROTOLIST((void));
extern int tet_xdlogoff PROTOLIST((void));
TET_IMPORT_FUNC(int, tet_xdlogon, PROTOLIST((void)));
extern char *tet_xdmsgbuf PROTOLIST((int));
extern char **tet_xdrcfname PROTOLIST((void));
extern int tet_xdresult PROTOLIST((long, int));
extern char *tet_xdtalk PROTOLIST((int, int));
TET_IMPORT_FUNC(int, tet_xdtpend, PROTOLIST((long)));
TET_IMPORT_FUNC(int, tet_xdtpstart, PROTOLIST((long, int)));
extern int tet_xdxfile PROTOLIST((char *, char *, int));
extern int tet_xdxrclose PROTOLIST((long));
TET_IMPORT_FUNC(int, tet_xdxres, PROTOLIST((long, char *)));
extern int tet_xdxresv PROTOLIST((long, char **, int));
extern long tet_xdxropen PROTOLIST((char *, int));
TET_IMPORT_FUNC(int, tet_xdxrsend, PROTOLIST((long)));
extern int tet_xdxrsys PROTOLIST((long, int *, int));

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

