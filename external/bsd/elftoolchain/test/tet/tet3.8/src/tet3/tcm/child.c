/*
 *      SCCS:  @(#)child.c	1.25 (02/05/15) 
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

#ifndef lint
static char sccsid_child[] = "@(#)child.c	1.25 (02/05/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)child.c	1.25 02/05/15 TETware release 3.8
NAME:		child.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1992

DESCRIPTION:
	tet_tcmc_main() is called from a different main() wrapper for each API,
	providing the main() function for processes started by tet_exec()
	and tet_remexec().

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1993
	removed reference to dapi.h

	Geoff Clare, UniSoft Ltd., August 1996
	Changes for TETWare.

	Geoff Clare, UniSoft Ltd., 21 August 1996
	Made include of <stdio.h> non-conditional.

	Geoff Clare, UniSoft Ltd, Sept 1996
	Moved tet_blockable_sigs setup to tet_init_blockable_sigs().
	Changes for TETware-Lite.

	Geoff Clare, UniSoft Ltd., Oct 1996
	restructured tcm source to avoid "ld -r"

	Geoff Clare, UniSoft Ltd., July 1997
	Changes to support NT threads.

	Andrew Dingwall, UniSoft Ltd., July 1997
	Added code to generate a new (unique) context value on Win32 systems,
	since it is not possible for tet_spawn() to call tet_setcontext()
	on a Win32 system.

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
	Note that this includes a change to the calling convention for
	child processes.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	restructured to enable tcm.o and friends to be built using
	the ld -r hack on Win32 systems

	Andrew Dingwall, UniSoft Ltd., March 2000
	set context and block in the calling function

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
**	+ The only .h file that may appear in this file is tcmhdrs.h;
**	  all other .h files that are needed must be #included in there.
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
#  define srcFile srcFile_child
   static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* global variables, values inhereted from tet_exec() and tet_remexec() */
#ifdef TET_THREADS
static	int	start_errno;
static	int	start_child;
static	int	start_alrm_flag;
static	long	start_block;
static	long	start_sequence;
#endif /* TET_THREADS */


#ifdef PROTOTYPES
int tet_tcmc_main(int argc, char **argv)
#else
int tet_tcmc_main(argc, argv)
int argc;
char **argv;
#endif
{
	register int rc;
#ifdef _WIN32	/* -START-WIN32-CUT- */
	struct _timeb timeb;
#endif		/* -END-WIN32-CUT- */


#ifdef TET_THREADS
	/* initial thread ID */
	tet_start_tid = TET_THR_SELF();

	/* set up thread-specific data */
	if (TET_THR_KEYCREATE(&tet_errno_key) != 0 ||
#  ifndef _WIN32	/* -WIN32-CUT-LINE- */
	    TET_THR_KEYCREATE(&tet_child_key) != 0 ||
	    TET_THR_KEYCREATE(&tet_alarm_flag_key) != 0 ||
#  endif		/* -WIN32-CUT-LINE- */
	    TET_THR_KEYCREATE(&tet_block_key) != 0 ||
	    TET_THR_KEYCREATE(&tet_sequence_key) != 0)
	{
		/* can't use tet_error() yet */
		(void) fprintf(stderr,
			"%s: TET_THR_KEYCREATE() failed in TCM startup",
			tet_progname);
		exit(EXIT_FAILURE);
	}
	TET_THR_SETSPECIFIC(tet_errno_key, (void *) &start_errno);
#  ifndef _WIN32	/* -WIN32-CUT-LINE- */
	TET_THR_SETSPECIFIC(tet_child_key, (void *) &start_child);
	TET_THR_SETSPECIFIC(tet_alarm_flag_key, (void *) &start_alrm_flag);
#  endif		/* -WIN32-CUT-LINE- */
	TET_THR_SETSPECIFIC(tet_block_key, (void *) &start_block);
	TET_THR_SETSPECIFIC(tet_sequence_key, (void *) &start_sequence);

	/* initialise all mutexes */
	tet_mtx_init();
#endif

	if (argc < TET_TCMC_USER_ARGS + 1)
		fatal(0, "argument count wrong; process must be executed by",
			tet_callfuncname());

	/*
	** set the global variables from the command line -
	** on WIN32 systems there is no fork() in the parent process so
	** tet_setcontext() and tet_setblock() have not yet been called;
	** so argv[3] and argv[4] are the context and block values from the
	** parent and we must simulate calls to tet_setcontext() and
	** tet_setblock() in this process
	**
	** the pids get reused too quickly on WIN32 systems to be useful
	** as a context value;
	** we do the best we can to generate a unique context value
	** which can be used to distinguish journal output from that
	** of other processes that write to the same journal
	**
	** on UNIX systems the context and block number must be set in the
	** calling function
	*/
	tet_thistest = atoi(*(argv + TET_TCMC_THISTEST));
	tet_activity = atol(*(argv + TET_TCMC_ACTIVITY));
#ifdef _WIN32		/* -START-WIN32-CUT- */
	do {
		_ftime(&timeb);
		tet_context = (long) (((((((unsigned long) timeb.time % 100000L) * 100L) + ((timeb.millitm / 10) % 100)) * (unsigned long) tet_mypid)) % 10000000L);
	} while (tet_context == atol(*(argv+ TET_TCMC_CONTEXT)));
#  ifdef TET_THREADS
	tet_next_block = 0;
	tet_setblock();
#  else
	tet_block = 1;
#  endif
	tet_sequence = 1;
#endif /* _WIN32 */	/* -END-WIN32-CUT- */

	argc -= TET_TCMC_USER_ARGS;
	argv += TET_TCMC_USER_ARGS;
	tet_pname = *argv;

	tet_init_blockable_sigs();

	/* initialise the server and transport stuff */
	tet_tcminit(argc, argv);

	/* start a new block and read in the config variables */
	tet_setblock();
	tet_config();

	/* call the user-supplied program */
	rc = tet_main(argc, argv);

#ifdef TET_THREADS
	tet_cln_threads(0);
	tet_mtx_destroy();
	tet_mtx_init();
#endif

	/* log off all the servers and exit */
	tet_logoff();
	return(rc);
}

