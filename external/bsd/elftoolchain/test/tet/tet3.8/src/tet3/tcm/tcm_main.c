/*
 *	SCCS: @(#)tcm_main.c	1.4 (02/05/15)
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

/*
 * Copyright 1990 Open Software Foundation (OSF)
 * Copyright 1990 Unix International (UI)
 * Copyright 1990 X/Open Company Limited (X/Open)
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of OSF, UI or X/Open not be used in 
 * advertising or publicity pertaining to distribution of the software 
 * without specific, written prior permission.  OSF, UI and X/Open make 
 * no representations about the suitability of this software for any purpose.  
 * It is provided "as is" without express or implied warranty.
 *
 * OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
 * EVENT SHALL OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
 * USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
 * PERFORMANCE OF THIS SOFTWARE.
 */

#ifndef lint
static char sccsid_tcm_main[] = "@(#)tcm_main.c	1.4 (02/05/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcm_main.c	1.4 02/05/15 TETware release 3.8
NAME:		'C' Test Case Manager
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	23 July 1990
SYNOPSIS:

	int tet_tcm_main(int argc, char **argv);
	int tet_nosigreset;

DESCRIPTION:

	Tcm_main() is the main program for the Test Case Manager (TCM),
	called from a different main() wrapper for each API.
	The command line contains the invocable components (IC's) to be
	executed.  All test purposes in the user-supplied tet_testlist[]
	array with an icref element corresponding to one of the requested
	IC's are called, preceded by a call to the function pointed to
	by tet_startup and followed by tet_cleanup (if each is not NULL).
	The TCM reads configuration variables from the file specified
	by the environment variable TET_CONFIG, provides handling of
	unexpected signals on UNIX-like platforms, and reports the start
	and end of IC's and the start of test purposes.

	Tet_thistest is set to the test purpose number during execution
	of each test purpose.  Test purposes are numbered in the sequence
	of the tet_testlist[] array (starting at 1) regardless of
	execution sequence.

MODIFICATIONS:

	Geoff Clare, UniSoft Ltd, 6 Sept 1990
		New results file format.
		If no arguments assume all ICs.
		Move tet_error() to resfile.c.
		Change handling of ICs with zero TPs.

	Geoff Clare, UniSoft Ltd, 28 Sept 1990
		When doing all ICs start from lowest defined IC instead of 1.
	
	Geoff Clare, UniSoft Ltd, 28 Nov 1990
		Add tet_nosigreset.
	
	Geoff Clare, UniSoft Ltd, 6 Sept 1991
		Add call to tet_delreas().

	DTET development - this file is based on release 1.10 of the TET
	David G. Sawyer and John-Paul Leyland, UniSoft Ltd, June 1992

	Andrew Dingwall, UniSoft Ltd., October 1992
	Moved tet_root[] and setup to tcmfuncs.c so that tcmchild
	processes can see it as well.

	Updated Abort, Delete and results processing to work in a DTET
	environment.

	Geoff Clare, UniSoft Ltd, July-August 1996
	Changes for TETWare.

	Andrew Dingwall, UniSoft Ltd., August 1996
	changes for use with tetware tcc

	Geoff Clare, UniSoft Ltd, Sept 1996
	Added call to tet_init_blockable_sigs().
	Changes for TETWare-Lite.

	Geoff Clare, UniSoft Ltd., Oct 1996
	Use TET_THR_EQUAL() to compare thread IDs.
	Restructured tcm source to avoid "ld -r".

	Andrew Dingwall, UniSoft Ltd., June 1997
	Changes to support the defined interface between the TCM and
	the test case.
	Changed the sigsetjmp()/siglongjmp() code so that the signal number
	is passed back in a global variable rather than by using the 2nd
	argument to siglongjmp();
	(apparently the only thing that can be done with the return value of
	sigsetjmp() is to perform a comparison on it - for maximum portability
	it should not be assigned to a variable).

	Geoff Clare, UniSoft Ltd., July 1997
	Changes to support NT threads.

	Andrew Dingwall, UniSoft Ltd., January 1998
	Permit the iclist to include IC 0.

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.
	Handle an empty iclist correctly.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

	Andrew Dingwall, UniSoft Ltd., November 2000
	Added support for TET_RTSIG_IGN and TET_RTSIG_LEAVE.

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
#  define srcFile srcFile_tcm2
   static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


#ifdef TET_LITE /* -LITE-CUT-LINE- */
#  define	VERSION		"3.8-lite"
#else /* -START-LITE-CUT- */
#  define	VERSION		"3.8"
#endif /* -END-LITE-CUT- */
#define	KILLWAIT	10

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

/* API global variables (only in the top-level TCM) */
int tet_nosigreset = 0;			/* has no effect in WIN32 */


/*
**	the IC list
**
**	an entry appears in the IC list for each IC number or range of
**	IC numbers that are to be executed
*/

/* structure of an entry in the IC list */
struct iclist {
	int ic_start;	/* low IC number in a range */
	int ic_end;	/* high IC number in a range */
};

/* the IC list itself */
static struct iclist *iclist;
static int liclist, niclist;


static	pid_t	toppid;

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
static	int	sigreset = 1;
static	int	signum;
static	sigjmp_buf skipjmp;
#endif		/* -WIN32-CUT-LINE- */

#ifdef TET_THREADS
static	int	start_errno;
static	int	start_child;
static	int	start_alrm_flag;
static	long	start_block;
static	long	start_sequence;
#endif /* TET_THREADS */


/* static function declarations */
static void build_icl2 PROTOLIST((char *, int, int));
static void build_icl3 PROTOLIST((char *, int, int));
static void build_iclist PROTOLIST((char **, int));
static void call_1tp PROTOLIST((int, int, int));
static int call_tps PROTOLIST((int, int));
static struct iclist *iclalloc PROTOLIST((void));
static int split PROTOLIST((char *, char **, int, int));

#ifndef TET_LITE /* -START-LITE-CUT- */
   static void sync_report PROTOLIST((int, char *));
#endif		/* -END-LITE-CUT- */

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
   static void sig_init PROTOLIST((char *, sigset_t *));
#  if defined(TET_SIG_IGN) || defined(TET_SIG_LEAVE)
     static void internal_sig_init PROTOLIST((char *, int *, sigset_t *));
#  endif
#  if defined(SIGRTMIN) && defined(SIGRTMAX)
     static void rtsig_init PROTOLIST((char *, sigset_t *));
#    if defined(TET_RTSIG_IGN) || defined(TET_RTSIG_LEAVE)
       static void internal_rtsig_init PROTOLIST((char *, sigset_t *));
#    endif
#  endif
   static void sigskip PROTOLIST((int));
   static void sigabandon PROTOLIST((int));
   static void sigterm PROTOLIST((void));
   static void setsigs PROTOLIST((void (*) PROTOLIST((int))));
#endif		/* -WIN32-CUT-LINE- */


static	char	buf[256];

/* ARGSUSED */
#ifdef PROTOTYPES
void tet_tcm_main(int argc, char **argv)
#else
void tet_tcm_main(argc, argv)
int argc;
char **argv;
#endif
{
	char *cp;
	struct iclist *icp;
	int iccount, tpcount, icnum, rc;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	int nsys;
#endif			/* -END-LITE-CUT- */

#ifdef TET_LITE					/* -LITE-CUT-LINE- */
#  define MYPTYPE	PT_MTCM
#  define MYSYSID	0
#else						/* -START-LITE-CUT- */
#  define MYPTYPE	-1
#  define MYSYSID	-1
#endif						/* -END-LITE-CUT- */

	/* must be first */
	tet_api_status |= TET_API_INITIALISED;
	tet_init_globals(argc > 0 ? tet_basename(*argv) : "TCM",
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


	tet_pname = *argv;	/* used by tet_error() */

	/* save current PID for checking when unexpected signals
	   cause a longjmp() to main test loop */
	toppid = GETPID();

#ifdef TET_THREADS
	/* save initial thread ID for checking on unexpected signals */
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
			tet_pname);
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

	/* blockable signals reported "unexpected" if caught (updated later) */
	tet_init_blockable_sigs();

	/* set tet_activity from environment */
	cp = getenv("TET_ACTIVITY");
	if (cp == NULL || *cp == '\0')
		tet_activity = 0;
	else
		tet_activity = atol(cp);

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	/* open execution results file (do early, so tet_error() can use it) */
	tet_openres();
#endif /* TET_LITE */ /* -LITE-CUT-LINE- */

	/* initialise the server and transport stuff */
	tet_tcminit(argc, argv);

	/* read in configuration variables */
	tet_config();

	/* set current context to process ID */
	tet_setcontext();

	/* build the list of ICs to be executed */
	build_iclist(argv + 1, argc - 1);

	/* count the number of ICs in the list */
	iccount = 0;
	for (icp = iclist; icp < iclist + niclist; icp++) {
		TRACE3(tet_Ttcm, 8, "IC list element: start = %s, end = %s",
			tet_i2a(icp->ic_start), tet_i2a(icp->ic_end));
		for (icnum = icp->ic_start; icnum <= icp->ic_end; icnum++)
			if (tet_isdefic(icnum))
				iccount++;
	}

	/* generate the TCM Start line */
	tet_tcmstart(VERSION, iccount);

#ifndef _WIN32		/* -WIN32-CUT-LINE- */
	/* unexpected signals are fatal during startup */
	setsigs(sigabandon);
#endif			/* -WIN32-CUT-LINE- */

#ifndef TET_LITE /* -START-LITE-CUT- */
	/* Do an auto sync -> use ic = -1, tp = 1 */
	tet_init_synreq();
	nsys = tet_Nsname;
	if (tet_tcm_async(MK_ASPNO(-1, 1, S_ICSTART), SV_YES, SV_SYNC_TIMEOUT,
		tet_synreq, &nsys) < 0) {
			tet_error(tet_sderrno,
				"startup function Auto Sync failed");
			tet_exit(EXIT_FAILURE);
	}

	/* now, analyse the results of an unsuccessful auto sync */
	if (tet_sderrno != ER_OK) {
		sync_report(nsys, "startup");
		tet_exit(EXIT_FAILURE);
	}
#endif		/* -END-LITE-CUT- */

	/* call user-supplied startup function */
	if (tet_startup != NULL)
	{
		(*tet_startup)();
#ifdef TET_THREADS
		tet_cln_threads(0);
		tet_mtx_destroy();
		tet_mtx_init();
#endif
	}
	
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/* see whether tet_nosigreset was set in startup function */
	if (tet_nosigreset)
		sigreset = 0;
#endif		/* -WIN32-CUT-LINE- */
	
	/* execute ICs specified on command line */
	for (icp = iclist; icp < iclist + niclist; icp++)
		for (icnum = icp->ic_start; icnum <= icp->ic_end; icnum++)
			if (tet_isdefic(icnum)) {
				tpcount = tet_gettpcount(icnum);
				rc = tet_icstart(icnum, tpcount);
				ASSERT_LITE(rc == 0);
				if (rc < 0)
					tet_docleanup(EXIT_FAILURE);
				tpcount = call_tps(icnum, tpcount);
				tet_icend(icnum, tpcount);
			}

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/* unexpected signals are fatal during cleanup */
	setsigs(sigabandon);
#endif		/* -WIN32-CUT-LINE- */

	/* call the user-supplied cleanup routine (if any) and exit */
	tet_docleanup(EXIT_SUCCESS);

	/* NOTREACHED */
}

/*
**	call_tps() - call each test purpose function in the specified
**		invocable component
**
**	return the number of TPs actually executed
*/

#ifdef PROTOTYPES
static int call_tps(int icnum, int tpcount)
#else
static int call_tps(icnum, tpcount)
int icnum, tpcount;
#endif
{
	int testcount, testnum, tpnum;
#ifndef TET_LITE /* -START-LITE-CUT- */
	long spno;
#endif /* -END-LITE-CUT- */

	TRACE3(tet_Ttcm, 6, "call_tps(): icnum = %s, tpcount = %s",
		tet_i2a(icnum), tet_i2a(tpcount));

	testcount = 0;
	for (tpnum = 1; tpnum <= tpcount; tpnum++) {
#ifndef TET_LITE /* -START-LITE-CUT- */
		spno = MK_ASPNO(0, tpnum, S_TPSTART);
		if (EX_TPNO(spno) != tpnum) {
			tet_error(0, "too many TPs defined in this IC");
			break;
		}
#endif /* -END-LITE-CUT- */
		testnum = tet_gettestnum(icnum, tpnum);
		call_1tp(icnum, tpnum, testnum);
		testcount++;
	}

	return(testcount);
}

/*
**	call_1tp() - call a single test purpose function
*/

#ifdef PROTOTYPES
static void call_1tp(int icnum, int tpnum, int testnum)
#else
static void call_1tp(icnum, tpnum, testnum)
int icnum, tpnum, testnum;
#endif
{
	TRACE4(tet_Ttcm, 6, "call_1tp(): icnum = %s, tpnum = %s, testnum = %s",
		tet_i2a(icnum), tet_i2a(tpnum), tet_i2a(testnum));

	/* a new TP starts with a single thread */
	tet_api_status &= ~TET_API_MULTITHREAD;

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/* there is no signal handling in WIN32 */

	/* establish the return point for an unexpected signal */
	if (sigsetjmp(skipjmp, 1) != 0)
	{
		/* we've caught an unexpected signal! */
		(void) sprintf(buf, "unexpected signal %d (%s) received",
			signum, tet_signame(signum));
		tet_infoline(buf);
		tet_result(TET_UNRESOLVED);
		if (tet_child > 0)
		{
			(void) tet_killw(tet_child, KILLWAIT);
			tet_child = 0;
		}
#  ifdef TET_THREADS
		tet_cln_threads(signum);
		tet_mtx_destroy();
		tet_mtx_init();
#  endif /* TET_THREADS */

		/* if this is not the top level process, don't fall
		   through to TCM test purpose loop! */
		if (GETPID() != toppid)
			exit(EXIT_FAILURE);

		if (signum == SIGTERM)
			sigterm();
	}
	else
	{

#endif		/* -WIN32-CUT-LINE- */

		/*
		** normal (non-signal) processing of a TP function
		*/

		/* output test start message */
		tet_tpstart(icnum, tpnum, testnum);

		/* set global current test purpose indicator */
		tet_thistest = testnum;

		/* see if this TP has been deleted */
		if (tet_reason(testnum) != NULL)
		{
			/* TP deleted in this TCM */
			TRACE2(tet_Ttcm, 4, "TP %s deleted on this system",
				tet_i2a(testnum));
			tet_infoline(tet_reason(testnum));
			tet_result(TET_UNINITIATED);
		}
#ifndef TET_LITE /* -START-LITE-CUT- */
		else if (tet_sync_del != 0)
		{
			/* TP deleted in another TCM */
			TRACE2(tet_Ttcm, 4, "TP %s deleted on another system",
				tet_i2a(testnum));
			tet_result(TET_UNINITIATED);
			tet_sync_del = 0;
		}
#endif /* -END-LITE-CUT- */
		else
		{

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
			/* unexpected signals skip current test
			   NOTE: this is done before every test function
			   to ensure no "local" signal handlers are left
			   in place when skipping to the next test.
			   This safety feature can be disabled by setting
			   tet_nosigreset in the tet_startup function,
			   in which case unexpected signals in later
			   tests could go unnoticed. */
			if (sigreset)
				setsigs(sigskip);
#endif		/* -WIN32-CUT-LINE- */

			/* call the user-supplied test function */
			TRACE3(tet_Ttcm, 1,
				"about to call tet_invoketp(%s, %s)",
				tet_i2a(icnum), tet_i2a(tpnum));
			(void) tet_invoketp(icnum, tpnum);
#ifdef TET_THREADS
			tet_cln_threads(0);
			tet_mtx_destroy();
			tet_mtx_init();
#endif
			TRACE3(tet_Ttcm, 2, "tet_invoketp(%s, %s) RETURN",
				tet_i2a(icnum), tet_i2a(tpnum));
		}

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	}
#endif		/* -WIN32-CUT-LINE- */

	/*
	** finally, output the test result -
	** if the action code for the result is ABORT, call the user-supplied
	** cleanup function and exit
	*/
	if (tet_tpend(icnum, tpnum, testnum) < 0)
		tet_docleanup(EXIT_FAILURE);
}


#ifndef _WIN32		/* -WIN32-CUT-LINE- */

#ifdef PROTOTYPES
static void sigabandon(int sig)
#else
static void sigabandon(sig)
int sig;
#endif
{
	static char	mbuf[132];

	if (sig == SIGTERM)
		sigterm();

#  ifdef TET_THREADS
#    ifdef TET_STRICT_POSIX_THREADS
	/* tet_start_tid is invalid if this assertion is false */
	ASSERT(!IS_CHILD_OF_MULTITHREAD_PARENT);
#    endif
	if (!TET_THR_EQUAL(TET_THR_SELF(), tet_start_tid))
	{
		(void) TET_THR_KILL(tet_start_tid, sig);
		TET_THR_EXIT((void *)0);
	}
#  endif /* TET_THREADS */

	(void) sprintf(mbuf,
		"Abandoning testset: caught unexpected signal %d (%s)",
		sig, tet_signame(sig));
	tet_error(0, mbuf);

#  ifdef TET_LITE	/* -LITE-CUT-LINE- */
	if (tet_tmpresfile != NULL)
		(void) UNLINK(tet_tmpresfile);
#  endif		/* -LITE-CUT-LINE- */

	/* log off all the servers and exit */
	tet_exit(EXIT_FAILURE);
}

#ifdef PROTOTYPES
static void sigterm(void)
#else
static void sigterm()
#endif
{
	/*  Cleanup and exit if SIGTERM received */

	char *msg = "Abandoning test case: received signal SIGTERM";

	/* terminate [per-thread] child, if any */
	if (tet_child > 0)
	{
		(void) tet_killw(tet_child, KILLWAIT);
		tet_child = 0;
	}

#  ifdef TET_THREADS
#    ifdef TET_STRICT_POSIX_THREADS
	/* tet_start_tid is invalid if this assertion is false */
	ASSERT(!IS_CHILD_OF_MULTITHREAD_PARENT);
#    endif
	if (!TET_THR_EQUAL(TET_THR_SELF(), tet_start_tid))
	{
		(void) TET_THR_KILL(tet_start_tid, SIGTERM);
		TET_THR_EXIT((void *)0);
	}

	/* only main thread gets to here */
	tet_cln_threads(SIGTERM);
#  endif /* TET_THREADS */

	tet_error(0, msg);

#  ifdef TET_LITE	/* -LITE-CUT-LINE- */
	if (tet_tmpresfile != NULL)
		(void) UNLINK(tet_tmpresfile);
#  endif /* TET_LITE */	/* -LITE-CUT-LINE- */

	/* call user-supplied cleanup function */
	if (tet_cleanup != NULL)
	{
		tet_thistest = 0;
#ifndef TET_THREADS
		tet_block = 0;
#else
		tet_next_block = 0;
#endif
		tet_setblock();

		(*tet_cleanup)();
#  ifdef TET_THREADS
		tet_cln_threads(0);
		tet_mtx_destroy();
		tet_mtx_init();  /* needed by tet_exit() */
#  endif /* TET_THREADS */
	}

	/* log off all the servers and exit */
	tet_exit(EXIT_FAILURE);
}


#ifdef PROTOTYPES
static void sigskip(int sig)
#else
static void sigskip(sig)
int sig;
#endif
{
	/*
	 * Catch unexpected signals and (in main thread) longjmp() to
	 * skipjmp where the setjmp() return value will be non-zero with
	 * the signal number in signum.
	 */

#  ifdef TET_THREADS
#    ifdef TET_STRICT_POSIX_THREADS
	/* tet_start_tid is invalid if this assertion is false */
	ASSERT(!IS_CHILD_OF_MULTITHREAD_PARENT);
#    endif
	if (!TET_THR_EQUAL(TET_THR_SELF(), tet_start_tid))
	{
		(void) TET_THR_KILL(tet_start_tid, sig);
		TET_THR_EXIT((void *)0);
	}
#  endif /* TET_THREADS */

	signum = sig;
	siglongjmp(skipjmp, 1);
}


#ifdef PROTOTYPES
static void sig_init(char *var, sigset_t *set)
#else
static void sig_init(var, set)
char *var;
sigset_t *set;
#endif
{
	/* initialise signal set from list in specified variable */

	/* note that this routine uses strtok() which will alter the
	   contents of the list variable */

	char	*list, *sname;
	int	snum;

	(void) sigemptyset(set);

	list = tet_getvar(var);
	if (list == NULL)
		return;

	for (sname = strtok(list, ", "); sname != NULL;
					sname = strtok((char *) NULL, ", "))
	{
		snum = atoi(sname);

		/* Check it's not a standard signal */
		if (strncmp(tet_signame(snum), "SIG", (size_t)3) == 0)
		{
			(void) sprintf(buf,
			    "warning: illegal entry \"%s\" in %s ignored",
			    sname, var);
			tet_error(0, buf);
		}
		else if (sigaddset(set, snum) == -1)
		{
			(void) sprintf(buf,
			    "warning: sigaddset() failed on entry \"%s\" in %s",
			    sname, var);
			tet_error(0, buf);
		}
	}
}

#if defined(SIGRTMIN) && defined(SIGRTMAX)
#  ifdef PROTOTYPES
static void rtsig_init(char *var, sigset_t *set)
#  else
static void rtsig_init(var, set)
char *var;
sigset_t *set;
#  endif
{
	int snum;
	int sigrtmin = SIGRTMIN;
	int sigrtmax = SIGRTMAX;
	char *val;

	val = tet_getvar(var);
	if (val == (char *) 0)
		return;

	switch (*val) {
	case 'T':
	case 't':
		break;
	default:
		return;
	}

	for (snum = sigrtmin; snum <= sigrtmax; snum++)
		if (sigaddset(set, snum) == -1) {
			(void) sprintf(buf,
			"warning: sigaddset() failed for signal %d in %s",
				snum, var);
			tet_error(0, buf);
		}
}
#endif

#if defined(TET_SIG_IGN) || defined(TET_SIG_LEAVE)
#  ifdef PROTOTYPES
static void internal_sig_init(char *name, int *sigp, sigset_t *set)
#  else
static void internal_sig_init(name, sigp, set)
char *name;
int *sigp;
sigset_t *set;
#  endif
{
	int err;

	for (; *sigp; sigp++) {
		TRACE3(tet_Ttcm, 4,
			"processing signal %s from compiled-in value of %s",
			tet_i2a(*sigp), name);
		if (!sigismember(set, *sigp) && sigaddset(set, *sigp) == -1) {
			err = errno;
			(void) sprintf(buf, "warning: sigaddset() failed for signal %d taken from compiled-in value of %s", *sigp, name);
			tet_error(err, buf);
		}
	}
}
#endif

#if defined(TET_RTSIG_IGN) || defined(TET_RTSIG_LEAVE)
#  if defined(SIGRTMIN) && defined(SIGRTMAX)
#    ifdef PROTOTYPES
static void internal_rtsig_init(char *name, sigset_t *set)
#    else
static void internal_rtsig_init(name, set)
char *name;
sigset_t *set;
#    endif
{
	int snum;
	int sigrtmin = SIGRTMIN;
	int sigrtmax = SIGRTMAX;

	for (snum = sigrtmin; snum <= sigrtmax; snum++) {
		TRACE3(tet_Ttcm, 4, "processing signal %s derived from %s",
			tet_i2a(snum), name);
		if (sigaddset(set, snum) == -1) {
			(void) sprintf(buf, "warning: sigaddset() failed for signal %d derived from %s",
				snum, name);
			tet_error(0, buf);
		}
	}
}
#  endif
#endif

#ifdef PROTOTYPES
static void setsigs(void (*func)(int))
#else
static void setsigs(func)
void (*func)();
#endif
{
	/*
	 * Sets all signals except SIGKILL, SIGSTOP and SIGCHLD
	 * to be caught by "func", except that signals specified in
	 * TET_SIG_IGN are ignored and signals specified in
	 * TET_SIG_LEAVE are left alone.
	 */

	int i;
	struct sigaction sig;
	static sigset_t	sig_ign;
	static sigset_t	sig_leave;
	static int	init_done = 0;
	static char ign_name[] = "TET_SIG_IGN";
	static char leave_name[] = "TET_SIG_LEAVE";
#if defined(SIGRTMIN) && defined(SIGRTMAX)
	static char rt_ign_name[] = "TET_RTSIG_IGN";
	static char rt_leave_name[] = "TET_RTSIG_LEAVE";
#endif

#ifdef TET_SIG_IGN
	static int internal_sig_ign[] = { TET_SIG_IGN, 0 };
#endif
#ifdef TET_SIG_LEAVE
	static int internal_sig_leave[] = { TET_SIG_LEAVE, 0 };
#endif

	if (!init_done)
	{
		sig_init(ign_name, &sig_ign);
		sig_init(leave_name, &sig_leave);
#if defined(SIGRTMIN) && defined(SIGRTMAX)
		rtsig_init(rt_ign_name, &sig_ign);
		rtsig_init(rt_leave_name, &sig_leave);
#endif
#ifdef TET_SIG_IGN
		internal_sig_init(ign_name, internal_sig_ign, &sig_ign);
#endif
#ifdef TET_SIG_LEAVE
		internal_sig_init(leave_name, internal_sig_leave, &sig_leave);
#endif
#if defined(SIGRTMIN) && defined(SIGRTMAX)
#  ifdef TET_RTSIG_IGN
		internal_rtsig_init(rt_ign_name, &sig_ign);
#  endif
#  ifdef TET_RTSIG_LEAVE
		internal_rtsig_init(rt_leave_name, &sig_leave);
#  endif
#endif
		init_done = 1;
	}

	(void) sigemptyset(&tet_blockable_sigs);

	/* NSIG is not provided by POSIX.1:  it must be defined via
	   an extra feature-test macro, or on the compiler command line */
	for (i = 1; i < NSIG; i++)
	{
		if (i == SIGKILL || i == SIGSTOP || i == SIGCHLD
					|| sigismember(&sig_leave, i))
			continue;
		
		if (sigismember(&sig_ign, i))
			sig.sa_handler = SIG_IGN;
		else
			sig.sa_handler = func;
		sig.sa_flags = 0;
		(void) sigemptyset(&sig.sa_mask);
		if (sigaction(i, &sig, (struct sigaction *)NULL) == 0 &&
#  ifdef SIGBUS
		    i != SIGBUS &&
#  endif
		    i != SIGSEGV && i != SIGILL && i != SIGFPE)
			(void) sigaddset(&tet_blockable_sigs, i);
	}
}

#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */


/*
**	tet_docleanup() - call the user-supplied cleanup function (if any)
**		and exit
*/

#ifdef PROTOTYPES
void tet_docleanup(int status)
#else
void tet_docleanup(status)
int status;
#endif
{
#ifndef TET_LITE /* -START-LITE-CUT- */
	int nsys;

	/* Do an auto sync -> use ic = tet_iclast + 1, tp = 1 */
	ASSERT(tet_synreq);
	nsys = tet_Nsname;
	if (tet_tcm_async(MK_ASPNO(tet_iclast + 1, 1, S_TPSTART), SV_YES,
		SV_SYNC_TIMEOUT, tet_synreq, &nsys) < 0) {
			tet_error(tet_sderrno,
				"cleanup function Auto Sync failed");
			tet_exit(EXIT_FAILURE);
	}

	/* analyse the results of an unsuccessful auto sync */
	if (tet_sderrno != ER_OK) {
		sync_report(nsys, "cleanup");
		tet_exit(EXIT_FAILURE);
	}
#endif /* -END-LITE-CUT- */

	/* call user-supplied cleanup function */
	if (tet_cleanup != NULL)
	{
		tet_thistest = 0;
#ifndef TET_THREADS
		tet_block = 0;
#else
		tet_next_block = 0;
#endif
		tet_setblock();

		(*tet_cleanup)();

#ifdef TET_THREADS
		tet_cln_threads(0);
		tet_mtx_destroy();
		tet_mtx_init();
#endif
	}

	tet_exit(status);
}


#ifndef TET_LITE /* -START-LITE-CUT- */

/*
**	sync_report() - analyse the results of the startup and cleanup
**		tet_tcm_async() calls
*/

#ifdef PROTOTYPES
static void sync_report(int nsys, char *functype)
#else
static void sync_report(nsys, functype)
register int nsys;
char *functype;
#endif
{
	register struct synreq *sp;
	char errmsg[128];

	for (sp = tet_synreq; sp < tet_synreq + nsys; sp++)
		switch (sp->sy_state) {
		case SS_SYNCYES:
			break;
		default:
			(void) sprintf(errmsg,
		"%s function Auto Sync error, sysid = %d, state = %s",
				functype, sp->sy_sysid,
				tet_systate(sp->sy_state));
			tet_error(tet_sderrno, errmsg);
			break;
		}
}

#endif /* -END-LITE-CUT- */

/*
**	build_iclist() - build the list of ICs to be executed
**
**	the IC specifications are taken from TCM command line;
**	each specification can contain one or more comma-separated elements;
**	each element can be an IC number (n), a range of IC numbers (m-n),
**	or the word "all"
**
**	when an element consists of a range of IC numbers (m-n),
**	a missing m means the lowest numbered IC defined in the test case
**	and a missing n means the highest numbered IC defined in the
**	test case
**
**	when the word "all" appears and is not the first element, it means
**	all of the ICs beyond the highest IC specified in the previous element
*/

#ifdef PROTOTYPES
static void build_iclist(char **icspec, int nicspec)
#else
static void build_iclist(icspec, nicspec)
char **icspec;
int nicspec;
#endif
{
	static char fmt[] = "tet_get%sic() returns %d but tet_isdefic(%d) returns FALSE!";
	char msg[sizeof fmt + (LNUMSZ * 2)];
	int icmin, icmax;
	int err;
#ifndef TET_LITE /* -START-LITE-CUT- */
	long spno;
#endif /* -END-LITE-CUT- */

	/* determin the lowest and highest IC defined in this test case */
	icmin = tet_getminic();
	icmax = tet_getmaxic();
	TRACE3(tet_Ttcm, 8, "build_iclist(): icmin = %s, icmax = %s",
		tet_i2a(icmin), tet_i2a(icmax));

	/* return now if the iclist is empty */
	if (icmin <= 0 && icmax == icmin && !tet_isdefic(icmin))
		return;

	/*
	** ensure that tet_isdefic() says that icmin and icmax have been
	** defined!
	*/
	err = 0;
	if (!tet_isdefic(icmin)) {
		(void) sprintf(msg, fmt, "min", icmin, icmin);
		tet_error(0, msg);
		err = 1;
	}
	if (!tet_isdefic(icmax)) {
		(void) sprintf(msg, fmt, "max", icmax, icmax);
		tet_error(0, msg);
		icmax = 1;
		err = 1;
	}

#ifndef TET_LITE /* -START-LITE-CUT- */
	/* ensure that the largest IC is within bounds */
	tet_iclast = icmax;
	spno = MK_ASPNO(tet_iclast + 1, 0, S_ICSTART);
	if (spno < 0L || EX_ICNO(spno) != tet_iclast + 1) {
		tet_error(0, "the largest IC number defined in this test case is too big");
		err = 1;
	}
#endif /* -END-LITE-CUT- */

	if (err)
		tet_exit(EXIT_FAILURE);

	/*
	** if we have one or more IC specifications, use them to build the
	** IC list; otherwise, build the list from all the ICs defined
	** in this test case
	*/
	if (nicspec > 0)
		while (nicspec-- > 0)
			build_icl2(*icspec++, icmin, icmax);
	else
		build_icl3("all", icmin, icmax);
}

/*
**	build_icl2() - extend the build_iclist() processing for
**		a group of elements in a single specification
*/

#ifdef PROTOTYPES
static void build_icl2(char *icspec, int icmin, int icmax)
#else
static void build_icl2(icspec, icmin, icmax)
char *icspec;
int icmin, icmax;
#endif
{
	char buf[(LNUMSZ * 2) + 2];
	register char *p;

	TRACE2(tet_Ttcm, 8, "build_icl2(): icspec = \"%s\"", icspec);

	/* process each comma-separated element in turn */
	while (*icspec) {
		for (p = icspec; *p; p++)
			if (*p == ',')
				break;
		(void) sprintf(buf, "%.*s",
			TET_MIN((int) (p - icspec), (int) sizeof buf - 1),
			icspec);
		build_icl3(buf, icmin, icmax);
		icspec = p;
		if (*icspec)
			icspec++;
	}
}

/*
**	build_icl3() - extend the build_iclist() processing some more
**
**	process an individual IC number or number range
*/

#ifdef PROTOTYPES
static void build_icl3(char *icspec, int icmin, int icmax)
#else
static void build_icl3(icspec, icmin, icmax)
char *icspec;
int icmin, icmax;
#endif
{
	static int last_icend = -1;
	int icstart, icend;
	struct iclist *icp;
	char *flds[2];
	static char fmt[] = "IC %d is not defined for this test case";
	char msg[sizeof fmt + LNUMSZ];

	TRACE2(tet_Ttcm, 8, "build_icl3(): icspec = \"%s\"", icspec);

	/*
	** if the element is "all" and ICs exist beyond the highest IC
	** in the last element, add the rest of the ICs to the list
	** and return
	*/
	if (!strcmp(icspec, "all")) {
		if (last_icend == -1 || last_icend < icmax) {
			icp = iclalloc();
			icstart = TET_MAX(icmin, last_icend + 1);
			icend = icmax;
			if (last_icend >= 0)
				while (icstart < icend && !tet_isdefic(icstart))
					icstart++;
			icp->ic_start = icstart;
			icp->ic_end = icend;
			last_icend = icp->ic_end;
		}
		return;
	}

	/*
	** here if the element is a number or a number range
	**
	** split the element into '-' separated fields and extract the
	** number from each one
	*/
	switch (split(icspec, flds, (int) (sizeof flds / sizeof flds[0]), '-')) {
	case 1:
		icend = icstart = atoi(flds[0]);
		break;
	case 2:
		icstart = *flds[0] ? atoi(flds[0]) : icmin;
		icend = *flds[1] ? atoi(flds[1]) : icmax;
		break;
	default:
		return;
	}

	/*
	** find the lowest defined IC that is greater than or equal to
	** the specified start IC
	**
	** print a diagnostic if the specified start IC does not exist
	*/
	if (!tet_isdefic(icstart)) {
		(void) sprintf(msg, fmt, icstart);
		tet_error(0, msg);
		while (++icstart <= icend)
			if (tet_isdefic(icstart))
				break;
	}

	/*
	** return now if no ICs within the specified range have been defined
	** in the test case
	*/
	if (icstart > icend)
		return;

	/*
	** here, the IC referenced by the (possibly modified) icstart
	** is known to be defined
	**
	** find the highest defined IC that is less than or equal to
	** the specified end IC
	**
	** print a diagnostic if the specified end IC does not exist
	*/
	if (icstart != icend && !tet_isdefic(icend)) {
		(void) sprintf(msg, fmt, icend);
		tet_error(0, msg);
		while (--icend > icstart)
			if (tet_isdefic(icend))
				break;
	}

	/*
	** here we have a known good start and end IC
	** (which might be the same)
	**
	** add the IC range to the list and return
	*/
	icp = iclalloc();
	icp->ic_start = icstart;
	icp->ic_end = icend;
	last_icend = icp->ic_end;
}

/*
**	iclalloc() - allocate an IC list element and return a pointer thereto
*/

#ifdef PROTOTYPES
static struct iclist *iclalloc(void)
#else
static struct iclist *iclalloc()
#endif
{
	register struct iclist *icp;

	if (BUFCHK((char **) &iclist, &liclist, (niclist + 1) * sizeof *iclist) < 0)
		tet_exit(EXIT_FAILURE);

	icp = iclist + niclist++;
	bzero((char *) icp, sizeof *icp);
	return(icp);
}

/*
**	split() - split a string up into at most maxargs fields,
**		discarding excess fields
**
**	return the number of fields found
*/

#ifdef PROTOTYPES
static int split(char *s, char **argv, int maxargs, int delim)
#else
static int split(s, argv, maxargs, delim)
register char *s, **argv;
register int maxargs, delim;
#endif
{
	register char **ap = argv;

	if (!*s || maxargs <= 0)
		return(0);

	*ap++ = s;
	while (*s) {
		if (*s == (char) delim) {
			*s++ = '\0';
			if (ap < argv + maxargs)
				*ap++ = s;
			else
				break;
		}
		else
			s++;
	}

	return(ap - argv);
}



#ifndef TET_LITE /* -START-LITE-CUT- */

/*
**	tet_tcm_async() - do an automatic sync from the MTCM
*/

#ifdef PROTOTYPES
int tet_tcm_async(long spno, int vote, int timeout,
	struct synreq *synreq, int *nsys)
#else
int tet_tcm_async(spno, vote, timeout, synreq, nsys)
long spno;
int vote, timeout, *nsys;
struct synreq *synreq;
#endif
{
	return(tet_sdasync(tet_snid, tet_xrid, spno, vote, timeout, synreq, nsys));
}

/*
**	tet_tcmptype() - return process type for a TCM
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

#endif /* -END-LITE-CUT- */

