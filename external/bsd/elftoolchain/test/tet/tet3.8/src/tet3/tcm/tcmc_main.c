/*
 *	SCCS: @(#)tcmc_main.c	1.4 (00/04/03)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1999 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 *
 */

#ifndef lint
static char sccsid_tcmc_main[] = "@(#)tcmc_main.c	1.4 (00/04/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcmc_main.c	1.4 00/04/03 TETware release 3.8
NAME:		tcmc_main.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1999

DESCRIPTION:
	Entry point to the child process controller.

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

	Andrew Dingwall, UniSoft Ltd., March 2000
	On UNIX systems, set context and block number here before calling
	tet_tcmc_main().

************************************************************************/

/*
** This file is a component of the TCM (tcm.o) and/or one of the child
** process controllers (tcmchild.o and tcmrem.o).
** On UNIX systems, these .o files are built using ld -r.
** There is no equivalent to ld -r in MSVC, so on Win32 systems each .c
** file is #included in a scratch .c or .cpp file and a single object
** file built from that.
**
** This imposes some restictions on the contents of this file:
**
**	+ Since this file might be included in a C++ program, all
**	  functions must have both ANSI C and common C definitions.
**
**	+ The scope of static variables and functions encompasses all
**	  the source files, not just this file.
**	  So all static variables and functions must have unique names.
*/


/*
** all the header files are included by tcmhdrs.h
** don't include any other header files directly
*/
#include "tcmhdrs.h"


#ifdef NEEDsrcFile
#  ifdef srcFile
#    undef srcFile
#  endif
#  define srcFile srcFile_tcmc2
   static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	TCM/API global variables
*/
/* these are used internally by the TCM and/or API */
#ifndef TET_LITE /* -START-LITE-CUT- */
   TET_EXPORT struct ptab *tet_sdptab;	/* ptab element for syncd */
   TET_EXPORT struct ptab *tet_xdptab;	/* ptab element for xresd */
#endif		/* -END-LITE-CUT- */

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
   TET_EXPORT sigset_t tet_blockable_sigs;
#endif		/* -WIN32-CUT-LINE- */


/*
**	tet_tcmchild_main() - entry point for the child process controller
*/

#ifdef PROTOTYPES
void tet_tcmchild_main(int argc, char **argv)
#else
void tet_tcmchild_main(argc, argv)
int argc;
char **argv;
#endif
{

#ifdef TET_LITE					/* -LITE-CUT-LINE- */
#  define MYPTYPE	PT_MTCM
#  define MYSYSID	0
#else						/* -START-LITE-CUT- */
#  define MYPTYPE	-1
#  define MYSYSID	-1
#endif						/* -END-LITE-CUT- */

	/* must be first */
	tet_api_status |= TET_API_INITIALISED;
	tet_init_globals(argc > 0 ? tet_basename(*argv) : "child process controller",
		MYPTYPE, MYSYSID, tet_dtcmerror, tet_genfatal);

	/*
	** make sure that we are linked with the right version of
	** the API library
	*/
	tet_check_apilib_version();

#if defined(_WIN32) && defined(TET_SHLIB) 	/* -START-WIN32-CUT- */
	/* call the dynamic linker */
	tet_w32dynlink();
#endif						/* -END-WIN32-CUT- */

#ifndef _WIN32					/* - WIN32-CUT-LINE- */
	/* on UNIX systems, set the context and block number */
	tet_context = atol(*(argv + TET_TCMC_CONTEXT));
#  ifdef TET_THREADS
	tet_next_block = atol(*(argv + TET_TCMC_BLOCK));
#  else
	tet_block = atol(*(argv + TET_TCMC_BLOCK));
#  endif
#endif						/* - WIN32-CUT-LINE- */

	exit(tet_tcmc_main(argc, argv));
}


#ifndef TET_LITE /* -START-LITE-CUT- */

/*
**	tet_tcm_async() - dummy auto-sync function for child TCMs
**
**	child TCMs do not do auto-syncs, so this function should only be
**	called with spno == SV_EXEC_SPNO from tet_tcminit()
*/

/* ARGSUSED */
#ifdef PROTOTYPES
TET_EXPORT int tet_tcm_async(long spno, int vote, int timeout,
	    struct synreq *synreq, int *nsys)
#else
TET_EXPORT int tet_tcm_async(spno, vote, timeout, synreq, nsys)
long spno;
int vote, timeout, *nsys;
struct synreq *synreq;
#endif
{
	ASSERT(spno == SV_EXEC_SPNO);

	tet_sderrno = ER_OK;
	return(0);
}

/*
**	tet_tcmptype() - return process type for a child TCM
**
**	In TETware there is almost no difference between "master" and
**	"slave" TCMs and this function is only used to decide which
**	TCMs in a distributed test case should inform XRESD of IC and
**	TP start and end.
**	Someone has to do it so it might as well be the first (or only)
**	system in the system list.
*/

#ifdef PROTOTYPES
int tet_tcmptype(void)
#else
int tet_tcmptype()
#endif
{
	int sys1;

	sys1 = tet_snames ? *tet_snames : -1;

	return (tet_mysysid == sys1 ? PT_MTCM : PT_STCM);
}

#endif	/* -END-LITE-CUT- */


/*
**	tet_callfuncname() - return name of tcmchild's calling function
**		for use in error messages
*/

#ifdef PROTOTYPES
char *tet_callfuncname(void)
#else
char *tet_callfuncname()
#endif
{
#ifdef _WIN32	/* -START-WIN32-CUT- */
	return("tet_spawn()");
#else		/* -END-WIN32-CUT- */
	return("tet_exec() or tet_spawn()");
#endif		/* -WIN32-CUT-LINE- */
}


