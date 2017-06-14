/*
 *      SCCS:  @(#)dtetlib.h	1.23 (05/11/02)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Ltd.
 * (C) Copyright 2005 The Open Group
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

SCCS:   	@(#)dtetlib.h	1.23 05/11/02 TETware release 3.8
NAME:		dtetlib.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	declarations for extern dtetlib functions not declared in other
	header files

REQUIRES PRIOR INCLUSION OF:
	<stdio.h>	(for FILE)
	"dtmac.h"

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	added prototypes for the functions in fcopy.c

	Denis McConalogue, UniSoft Limited, September 1993
	added prototype for pmatch() in fcopy.c

	Andrew Dingwall, UniSoft Ltd., February 1994
	enhancements for FIFO interface

	Geoff Clare, UniSoft Ltd., Sept 1996
	Added functions from sigsafe.c

	Andrew Dingwall, UniSoft Ltd., May 1997
	added functions for the Windows 95 port

	Andrew Dingwall, UniSoft Ltd., July 1997
	added support the MT DLL version of the C runtime support library
	on Win32 systems

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Geoff Clare, The Open Group, June 2005
	Added tet_curtime().

        David Scholefield, The Open Group, November 2005
        Modified tet_curtime to be included in Win 32 DLLs.

************************************************************************/

/* extern function declarations */
extern char **tet_addargv PROTOLIST((char **, char **));
TET_IMPORT_FUNC(int, tet_addresult, PROTOLIST((int, int)));
TET_IMPORT_FUNC(char *, tet_basename, PROTOLIST((char *)));
TET_IMPORT_FUNC(int, tet_bufchk, PROTOLIST((char **, int *, int)));
TET_IMPORT_FUNC(int, tet_buftrace,
	PROTOLIST((char **, int *, int, char *, int)));
TET_IMPORT_FUNC(int, tet_curtime, PROTOLIST((char *, unsigned, int)));
extern int tet_eaccess PROTOLIST((char *, int));
extern char *tet_equindex PROTOLIST((char *));
extern char *tet_errname PROTOLIST((int));
extern int tet_fappend PROTOLIST((int));
extern int tet_fcopy PROTOLIST((char *, char *));
extern int tet_fgetargs PROTOLIST((FILE *, char **, int));
extern int tet_fioclex PROTOLIST((int));
extern void tet_generror PROTOLIST((int, char *, int, char *, char *));
TET_IMPORT_FUNC(void, tet_genfatal,
	PROTOLIST((int, char *, int, char *, char *)));
TET_IMPORT_FUNC(int, tet_getargs, PROTOLIST((char *, char **, int)));
extern int tet_getdtablesize PROTOLIST((void));
extern int tet_getrescode PROTOLIST((char *, int *));
extern char *tet_getresname PROTOLIST((int, int *));
extern void tet_hexdump PROTOLIST((char *, int, FILE *));
extern int tet_initrestab PROTOLIST((void));
extern char **tet_lsdir PROTOLIST((char *));
extern int tet_maperrno PROTOLIST((int));
extern int tet_mapsignal PROTOLIST((int));
extern int tet_mapstatus PROTOLIST((int));
extern int tet_mkalldirs PROTOLIST((char *));
extern int tet_mkdir PROTOLIST((char *, int));
extern int tet_mkoptarg PROTOLIST((char *, int, char *, int));
extern char *tet_mktfname PROTOLIST((char *));
extern int tet_pmatch PROTOLIST((char *, char *));
extern void tet_prerror PROTOLIST((FILE *, int, char *, char *, int, char *,
	char *));
extern char *tet_ptflags PROTOLIST((int));
TET_IMPORT_FUNC(char *, tet_ptptype, PROTOLIST((int)));
TET_IMPORT_FUNC(char *, tet_ptrepcode, PROTOLIST((int)));
TET_IMPORT_FUNC(char *, tet_ptreqcode, PROTOLIST((int)));
extern char *tet_ptstate PROTOLIST((int));
extern char *tet_ptsvote PROTOLIST((int));
TET_IMPORT_FUNC(int, tet_putenv, PROTOLIST((char *)));
extern int tet_readrescodes PROTOLIST((char *));
extern char *tet_remvar PROTOLIST((char *, int));
extern int tet_remvar_sysid PROTOLIST((char *));
extern int tet_rmdir PROTOLIST((char *));
TET_IMPORT_FUNC(char *, tet_strstore, PROTOLIST((char *)));
extern void tet_tiocnotty PROTOLIST((void));
extern int tet_unmaperrno PROTOLIST((int));
extern int tet_unmapsignal PROTOLIST((int));

#ifndef TET_LITE	/* -START-LITE-CUT- */
   TET_IMPORT_FUNC(char *, tet_systate, PROTOLIST((int)));
#endif			/* -END-LITE-CUT- */

#ifdef _WIN32		/* -START-WIN32-CUT- */
   extern int tet_iswin95 PROTOLIST((void));
   extern int tet_iswinNT PROTOLIST((void));
   extern int tet_spawnvpe PROTOLIST((char *, char **, char **));
   extern int tet_w32err2errno PROTOLIST((unsigned long));
#  ifndef TET_LITE	/* -START-LITE-CUT- */
      extern char *tet_wsaerrmsg PROTOLIST((int));
#  endif		/* -END-LITE-CUT- */
#else			/* -END-WIN32-CUT- */
   extern int tet_dofork PROTOLIST((void));
   extern int tet_dowait3 PROTOLIST((int *, int));
#endif			/* -WIN32-CUT-LINE- */

