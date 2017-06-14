/*
 *	SCCS: @(#)tcclib.h	1.4 (03/03/26)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

/************************************************************************

SCCS:   	@(#)tcclib.h	1.4 03/03/26 TETware release 3.8
NAME:		tcclib.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:
	declarations for tcc action functions in tcclib

	note that when this file is included in tcc source files,
	it must be included AFTER tcc.h (in which TCC is defined)

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

	Andrew Dingwall, The Open Group, March 2003
	Moved tcf_lsdir() to dtet2lib and renamed as tet_lsdir().
 
************************************************************************/

/*
** cause definitions of tcc action functions always to be visible in tccd
** and tcclib, but only visible in tcc when we are building TETware-Lite
*/
#ifdef TCC
#  ifdef TET_LITE	/* -LITE-CUT-LINE- */
#    define INCLUDE_TCCLIB_H	1
#  else			/* -START-LITE-CUT- */
#    define INCLUDE_TCCLIB_H	0
#  endif /* TET_LITE */	/* -END-LITE-CUT- */
#else
#  define INCLUDE_TCCLIB_H	1
#endif /* TCC */


/* only include this stuff when required */
#if INCLUDE_TCCLIB_H

/* macro to determine which trace flag to use in TRACEn() requests */
#  ifndef NOTRACE
#    ifdef TET_LITE	/* -LITE-CUT-LINE- */
#      define Ttcclib	tet_Ttcc
#    else		/* -START-LITE-CUT- */
#      define Ttcclib	((tet_myptype == PT_MTCC) ? tet_Ttcc : tet_Ttccd)
#    endif		/* -END-LITE-CUT- */
#  endif /* !NOTRACE */

/*
** values for the flag argument to tcf_procdir()
** these flags perform the same function as do the AV_TS_* flag values
** (defined in avmsg.h) which may be passed to tc_tsfiles()
*/
#  define TCF_TS_LOCAL	1	/* save files locally */
#  define TCF_TS_MASTER	2	/* transfer save files to the master system */

/*
** values for the flag argument to tcf_exec()
** these flags perform the same function as do the AV_EXEC_* flag values
** (defined in avmsg.h) which may be passed to tet_tcexec()
*/
#  define TCF_EXEC_TEST	1	/* a test case */
#  define TCF_EXEC_USER	2	/* a tcmrem process (from tet_remexec()) */
#  define TCF_EXEC_MISC	3	/* any other process */

/* extern function declarations */
extern int tcf_exec PROTOLIST((char *, char **, char *, long, int, int *));
extern int tcf_lockfile PROTOLIST((char *, int));
extern int tcf_mktmpdir PROTOLIST((char *, char **));
extern int tcf_procdir PROTOLIST((char *, char *, char *[], int, int));
extern int tcf_rmrf PROTOLIST((char *));
extern int tcf_sharelock PROTOLIST((char *, long, int, char **));

#  ifndef _WIN32	/* -WIN32-CUT-LINE- */
/* function called by tcf_exec() - supplied by tcc (lite) or tccd (full) */
extern void tcc_exec_signals PROTOLIST((void));
#  endif		/* -WIN32-CUT-LINE- */

#endif /* INCLUDE_TCCLIB_H */

