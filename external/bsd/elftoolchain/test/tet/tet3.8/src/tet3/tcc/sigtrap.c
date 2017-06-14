/*
 *	SCCS: @(#)sigtrap.c	1.7 (02/05/15)
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

#ifndef lint
static char sccsid[] = "@(#)sigtrap.c	1.7 (02/05/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sigtrap.c	1.7 02/05/15 TETware release 3.8
NAME:		sigtrap.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	September 1996

DESCRIPTION:
	tcc signal handling functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., May 1997
	Corrected a bug in the WIN32 signal blocking emulation.

	Andrew Dingwall, UniSoft Ltd., March 1998
	Arrange to interrupt looping directives on abort.

	Andrew Dingwall, UniSoft Ltd., March 2000
	Enhanced quick shutown on signal to perform better testcase
	termination and journal reporting.
	Fixed a problem in which sending a SIGHUP/SIGTERM to tcc while
	processing a remote or distributed test case on more than one
	system with output capture mode enabled caused an ASSERT()
	to fail when the child proctabs were freed.

************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <time.h>
#include <signal.h>
#include <errno.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "ltoa.h"
#include "servlib.h"
#include "scentab.h"
#include "proctab.h"
#include "tcc.h"
#include "tcclib.h"
#include "tet_jrnl.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* signal dispositions on startup */
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
static void (*orig_sigint) PROTOLIST((int));
static void (*orig_sighup) PROTOLIST((int));
static void (*orig_sigquit) PROTOLIST((int));
static void (*orig_sigpipe) PROTOLIST((int));
static void (*orig_sigterm) PROTOLIST((int));
#endif		/* -WIN32-CUT-LINE- */

/* variables used by the WIN32 sigblock emulation */
#ifdef _WIN32	/* -START-WIN32-CUT- */
static void (*intr_save) PROTOLIST((int));
static void (*break_save) PROTOLIST((int));
static int caught_intr;
static int caught_break;
static int sigs_blocked;
#endif		/* -END-WIN32-CUT- */

/* variable set by the quick_killtc mechanism to indicate that a
** sleep is needed
*/
static int do_sleep;


/* static function declarations */
static int eng1_tcinterrupt PROTOLIST((struct proctab *));
static void engine_abort PROTOLIST((int));
static int engine_tcinterrupt PROTOLIST((int));
static void engine_tcskip PROTOLIST((int));
static void initial_sigtrap PROTOLIST((int));
static void (*install_handler PROTOLIST((int, void (*) PROTOLIST((int)))))
	PROTOLIST((int));
static int quick_killtc PROTOLIST((struct proctab *));
static int quick_waittc PROTOLIST((struct proctab *));
#ifndef TET_LITE	/* -START-LITE-CUT- */
static void quick_wtc2 PROTOLIST((struct proctab *));
#endif			/* -END-LITE-CUT- */
static int quick_wtc3 PROTOLIST((struct proctab *));
#ifdef _WIN32		/* -START-WIN32-CUT- */
static void win32_sigtrap PROTOLIST((int));
#else			/* -END-WIN32-CUT- */
static int clear_outfile PROTOLIST((struct proctab *));
static void engine_sigterm PROTOLIST((int));
static void exec_sigprocmask PROTOLIST((int));
#  ifdef TET_LITE	/* -LITE-CUT-LINE- */
static void tes2 PROTOLIST((int, void (*) PROTOLIST((int))));
#  endif /* TET_LITE */	/* -LITE-CUT-LINE- */
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */


/*
**	initsigtrap() - install initial signal traps
*/

void initsigtrap()
{
#ifdef _WIN32	/* -START-WIN32-CUT- */
	(void) install_handler(SIGINT, initial_sigtrap);
	(void) install_handler(SIGBREAK, initial_sigtrap);
#else		/* -END-WIN32-CUT- */
	orig_sigint = install_handler(SIGINT, initial_sigtrap);
	orig_sighup = install_handler(SIGHUP, initial_sigtrap);
	orig_sigquit = install_handler(SIGQUIT, initial_sigtrap);
	orig_sigpipe = install_handler(SIGPIPE, initial_sigtrap);
	orig_sigterm = install_handler(SIGTERM, initial_sigtrap);
#endif		/* -WIN32-CUT-LINE- */
}

/*
**	initial_sigtrap() - signal handler for use before the execution
**		engine starts up
*/

static void initial_sigtrap(sig)
int sig;
{
	static char text[] = "TCC shutdown on signal";

#ifdef _WIN32	/* -START-WIN32-CUT- */
	(void) signal(sig, SIG_IGN);
#endif		/* -END-WIN32-CUT- */

	if (jnl_usable())
		(void) fprintf(stderr, "%s %d\n", text, sig);

	fatal(0, text, tet_i2a(sig));
}

/*
**	execsigtrap() - install signal traps for the execution engine
*/

void execsigtrap()
{
	exec_block_signals();
	(void) install_handler(SIGINT, engine_tcskip);
#ifdef _WIN32	/* -START-WIN32-CUT- */
	intr_save = engine_tcskip;
	(void) install_handler(SIGBREAK, engine_abort);
	break_save = engine_abort;
#else		/* -END-WIN32-CUT- */
	(void) install_handler(SIGHUP, engine_sigterm);
	(void) install_handler(SIGQUIT, engine_abort);
	(void) install_handler(SIGPIPE, engine_sigterm);
	(void) install_handler(SIGTERM, engine_sigterm);
#endif		/* -WIN32-CUT-LINE- */
}

/*
**	exec_block_signals() - block signals while the execution engine
**		turns over
*/

void exec_block_signals()
{
#ifdef _WIN32	/* -START-WIN32-CUT- */
	if (!sigs_blocked) {
		intr_save = signal(SIGINT, win32_sigtrap);
		break_save = signal(SIGBREAK, win32_sigtrap);
		caught_intr = caught_break = 0;
		sigs_blocked = 1;
	}
#else		/* -END-WIN32-CUT- */
	exec_sigprocmask(SIG_BLOCK);
#endif		/* -WIN32-CUT-LINE- */
}

void exec_unblock_signals()
{
#ifdef _WIN32	/* -START-WIN32-CUT- */
	if (sigs_blocked) {
		(void) signal(SIGBREAK, break_save);
		if (caught_break)
			raise(SIGBREAK);
		(void) signal(SIGINT, intr_save);
		if (caught_intr)
			raise(SIGINT);
		sigs_blocked = 0;
	}
#else		/* -END-WIN32-CUT- */
	exec_sigprocmask(SIG_UNBLOCK);
#endif		/* -WIN32-CUT-LINE- */
}

#ifdef _WIN32	/* -START-WIN32-CUT- */

/*
**	win32_sigtrap() - WIN32 signal handler for use when signals
**		are "blocked" during processing by the execution engine
*/

static void win32_sigtrap(sig)
int sig;
{
	(void) signal(sig, SIG_IGN);

	switch (sig) {
	case SIGINT:
		caught_intr = 1;
		break;
	case SIGBREAK:
		caught_break = 1;
		break;
	}

	(void) signal(sig, win32_sigtrap);
}

#endif		/* -END-WIN32-CUT- */


#ifndef _WIN32	/* -WIN32-CUT-LINE- */

/*
**	exec_sigprocmask() - block or unblock signals
*/

static void exec_sigprocmask(how)
int how;
{
	sigset_t mask;

	(void) sigemptyset(&mask);
	(void) sigaddset(&mask, SIGHUP);
	(void) sigaddset(&mask, SIGINT);
	(void) sigaddset(&mask, SIGQUIT);
	(void) sigaddset(&mask, SIGPIPE);
	(void) sigaddset(&mask, SIGTERM);

	if (sigprocmask(how, &mask, (sigset_t *) 0) < 0)
		fatal(errno, "sigprocmask() failed: how =", tet_i2a(how));
}

#endif		/* -WIN32-CUT-LINE- */


#ifndef _WIN32	/* -WIN32-CUT-LINE- */

/*
**	engine_sigterm() - SIGHUP and SIGTERM signal handler for use once
**		the execution engine is running
*/

static void engine_sigterm(sig)
int sig;
{
	struct proctab *prp;

	TRACE2(TET_MAX(tet_Ttcc, tet_Texec), 4, "engine_sigterm(): signal = %s",
		tet_i2a(sig));

	/*
	** there isn't really time to process an output capture file
	** or a journal just now
	*/
	for (prp = runq; prp; prp = prp->pr_rqforw)
		if (prp->pr_scen->sc_type == SC_TESTCASE) {
			(void) RUN_PROCTABS(prp, clear_outfile);
			if (prp->pr_jnlstatus == 0)
				prp->pr_jnlstatus = TET_ESTAT_ERROR;
		}

	initial_sigtrap(sig);
}

static int clear_outfile(prp)
struct proctab *prp;
{
	prp->pr_outfile = (char *) 0;
	return(0);
}

#endif		/* -WIN32-CUT-LINE- */


/*
**	engine_tcskip() - SIGINT signal handler for use once the execution
**		engine is running
*/

static void engine_tcskip(sig)
int sig;
{
#ifdef _WIN32	/* -START-WIN32-CUT- */
	(void) signal(sig, SIG_IGN);
#endif		/* -END-WIN32-CUT- */

	(void) fprintf(stderr, "TCC: user interrupt\n");
	(void) fflush(stderr);

	if (engine_tcinterrupt(sig) == 0) {
#ifdef _WIN32	/* -START-WIN32-CUT- */
		(void) fprintf(stderr, "TCC: no test cases to interrupt !\nTCC: try Ctrl-BREAK if you want to abort the tcc.\n\n");
#else		/* -END-WIN32-CUT- */
		(void) fprintf(stderr, "TCC: no test cases to interrupt !\nTCC: try SIGQUIT if you want to abort the tcc.\n\n");
#endif		/* -WIN32-CUT-LINE- */
		(void) fflush(stderr);
	}

#ifdef _WIN32	/* -START-WIN32-CUT- */
	(void) signal(sig, engine_tcskip);
#endif		/* -END-WIN32-CUT- */
}

/*
**	engine_abort() - SIGQUIT (or SIGBREAK) signal handler for use once
**		the execution engine is running
*/

static void engine_abort(sig)
int sig;
{
	register struct proctab *prp;

#ifdef _WIN32	/* -START-WIN32-CUT- */
	(void) signal(sig, SIG_IGN);
#endif		/* -END-WIN32-CUT- */

	(void) fprintf(stderr, "TCC: user abort called\n");
	(void) fflush(stderr);

	/*
	** this flag tells the execution engine not to start processing
	** any more test cases
	*/
	tcc_modes |= TCC_ABORT;

	/* arrange to interrupt each directive on the run queue */
	for (prp = runq; prp; prp = prp->pr_rqforw)
		if (prp->pr_scen->sc_type == SC_DIRECTIVE)
			prp->pr_modes |= TCC_ABORT;

	/*
	** tell the execution engine to interrupt all the currently
	** running test cases; the combination of these two actions
	** causes tcc to exit normally as soon as any currently running
	** test cases have terminated and any save files and journal
	** processing has completed
	*/
	(void) engine_tcinterrupt(sig);

#ifdef _WIN32	/* -START-WIN32-CUT- */
	(void) signal(sig, engine_abort);
#endif		/* -END-WIN32-CUT- */
}

/*
**	engine_tcinterrupt() - tell the execution engine to interrupt all the
**		currently running test case(s)
**
**	return the number of test cases currently running
**
**	this function is called from a signal handler, which itself
**	should only be called while the execution engine is turning over
**
**	note that the test cases are not actually interrupted until
**	control returns to the execution engine
*/

#ifdef NOTRACE
/* ARGSUSED */
#endif
static int engine_tcinterrupt(sig)
int sig;
{
	register struct proctab *prp;
	register int count = 0;

	TRACE2(TET_MAX(tet_Ttcc, tet_Texec), 4,
		"engine_tcinterrupt(): signal = %s", tet_i2a(sig));

	/* arrange to interrupt each testcase on the run queue */
	for (prp = runq; prp; prp = prp->pr_rqforw)
		if (prp->pr_scen->sc_type == SC_TESTCASE) {
			prp->pr_modes |= TCC_ABORT;
			if (prp->pr_state == PRS_WAIT) {
				count++;
				(void) RUN_PROCTABS(prp, eng1_tcinterrupt);
				prp->pr_flags |= PRF_ATTENTION;
			}
		}

	return(count);
}

/*
**	eng1_tcinterrupt() - extend the engine_tcinterrupt() processing for a
**		testcase or tool running on a single system
**
**	always returns 0
*/

static int eng1_tcinterrupt(prp)
register struct proctab *prp;
{
	TRACE4(TET_MAX(tet_Ttcc, tet_Texec), 6,
		"eng1_tcinterrupt(%s): tcstate = %s, toolstate = %s",
		tet_i2x(prp), prtcstate(prp->pr_tcstate),
		prtoolstate(prp->pr_toolstate));

	if (prp->pr_toolstate == PTS_RUNNING)
		prp->pr_toolstate = PTS_ABORT;

	return(0);
}

/*
**	engine_shutdown() - kill left-over test cases and remove lock
**		files when tcc is shutting down
**
**	this function is called during tcc's orderly shutdown processing;
**	control should not return to the execution engine after this
**	function has been called
**
**	when this function is called, there should only be left-over
**	test cases and lock files if the shutdown is being performed
**	under the control of a signal handler (eg: for SIGHUP or SIGTERM)
**
**	since control is not returned to the execution engine, any pending
**	save files and journal processing is not performed;
**	also, test cases are not waited for - if a test case does not
**	respond to SIGTERM (and SIGKILL on UNIX) it is left running
**	(however, in Distributed TETware, when tcc logs off each tccd,
**	tccd sends a SIGHUP to any left-over executed processes)
*/

void engine_shutdown()
{
	register struct proctab *prp;
	static int been_here = 0;

	TRACE3(TET_MAX(tet_Ttcc, tet_Texec), 4,
		"engine_shutdown(): been_here = %s, runq = %s",
		tet_i2a(been_here), tet_i2x(runq));

	/* guard against multiple calls and recursive calls */
	if (been_here++) {
		TRACE1(TET_MAX(tet_Ttcc, tet_Texec), 4,
			"engine_shutdown() quick RETURN");
		return;
	}

	/* kill each running testcase or tool */
	do_sleep = 0;
	for (prp = runq; prp; prp = prp->pr_rqforw)
		if (prp->pr_scen->sc_type == SC_TESTCASE)
			(void) RUN_PROCTABS(prp, quick_killtc);

	/* give the tools a chance to terminate */
	if (do_sleep)
		SLEEP(2);

	/*
	** determine journal status for each running testcase or tool
	** if possible
	*/
	do_sleep = 0;
	for (prp = runq; prp; prp = prp->pr_rqforw) {
		if (prp->pr_scen->sc_type != SC_TESTCASE)
			continue;
		TRACE3(TET_MAX(tet_Ttcc, tet_Texec), 6,
			"before quick_waittc(%s), jnlstatus = %s",
			tet_i2x(prp), tet_i2a(prp->pr_jnlstatus));
#ifndef TET_LITE	/* -START-LITE-CUT- */
		if (prp->pr_child) {
			(void) run_child_proctabs(prp, quick_waittc);
			if (
				(
					prp->pr_jnlstatus == 0 ||
					prp->pr_jnlstatus == TET_ESTAT_ERROR
				) &&
				(prp->pr_flags & PRF_JNL_CHILD) == 0
			)
				quick_wtc2(prp);
		}
		else
#endif			/* -END-LITE-CUT- */
			(void) quick_waittc(prp);
		TRACE3(TET_MAX(tet_Ttcc, tet_Texec), 6,
			"after quick_waittc(%s), jnlstatus = %s",
			tet_i2x(prp), tet_i2a(prp->pr_jnlstatus));
	}
	if (do_sleep)
		SLEEP(2);
	for (prp = runq; prp; prp = prp->pr_rqforw)
		if (prp->pr_scen->sc_type == SC_TESTCASE)
			(void) RUN_PROCTABS(prp, quick_wtc3);


	/* remove lock files, temporary directories and so forth */
	for (prp = runq; prp; prp = prp->pr_rqforw)
		if (prp->pr_scen->sc_type == SC_TESTCASE)
			switch (prp->pr_currmode) {
			case TCC_BUILD:
			case TCC_EXEC:
			case TCC_CLEAN:
				prp->pr_tcstate = TCS_END;
				proc_testcase(prp);
				break;
			}

	TRACE1(TET_MAX(tet_Ttcc, tet_Texec), 4,
		"engine_shutdown() normal RETURN");
}

/*
**	quick_killtc() - kill a test case or tool quickly on a single system
**		without reporting errors
**
**	this function is called when tcc performs a quick shutdown
**
**	sets the global variable do_sleep if a signal is sent
**	this tells the calling function to sleep a bit so as to give
**	child processes a chance to exit and servers to clean up
**
**	always returns 0
*/

static int quick_killtc(prp)
struct proctab *prp;
{
	TRACE5(TET_MAX(tet_Ttcc, tet_Texec), 6,
		"quick_killtc(%s): tcstate = %s, toolstate = %s, flags = %s",
		tet_i2x(prp), prtcstate(prp->pr_tcstate),
		prtoolstate(prp->pr_toolstate), prpflags(prp->pr_flags));

	if (prp->pr_toolstate == PTS_RUNNING) {
		if (tcc_kill(*prp->pr_sys, prp->pr_remid, SIGTERM) < 0)
			prp->pr_toolstate = PTS_UNWAITEDFOR;
		else
			prp->pr_toolstate = PTS_KILLED_AND_UNWAITEDFOR;
		do_sleep = 1;
	}

	return(0);
}

/*
**	quick_waittc() - wait for a test case or tool quickly on a single
**		system with minimal error checking
**
**	this function is called when tcc performs a quick shutdown
**	so there are a few short cuts here
**
**	sets the global variable do_sleep if a signal is sent
**	this tells the calling function to sleep a bit so as to give
**	child processes a chance to exit and servers to clean up
**
**	always returns 0
*/

static int quick_waittc(prp)
struct proctab *prp;
{
	int status;

	TRACE5(TET_MAX(tet_Ttcc, tet_Texec), 6,
		"quick_waittc(%s): tcstate = %s, toolstate = %s, flags = %s",
		tet_i2x(prp), prtcstate(prp->pr_tcstate),
		prtoolstate(prp->pr_toolstate), prpflags(prp->pr_flags));

	switch (prp->pr_toolstate) {
	case PTS_UNWAITEDFOR:
	case PTS_KILLED_AND_UNWAITEDFOR:
		break;
	default:
		return(0);
	}

	/*
	** determine the process exit status after a SIGTERM has been sent
	** (on UNIX systems) or after a call to TerminateProcess()
	** (on a Win32 system);
	** if the process is still running or can't be waited for,
	** contrive an exit code;
	** on UNIX systems, send a SIGKILL for good measure
	*/
	if (tcc_waitnohang(*prp->pr_sys, prp->pr_remid, &status) < 0) {
		switch (prp->pr_toolstate) {
		case PTS_UNWAITEDFOR:
			prp->pr_exitcode = 0177777;
			break;
		case PTS_KILLED_AND_UNWAITEDFOR:
			switch (tet_tcerrno) {
			case ER_WAIT:
#ifdef _WIN32	/* -START-WIN32-CUT- */
				prp->pr_exitcode = 3;
#else		/* -END-WIN32-CUT- */
				(void) tcc_kill(*prp->pr_sys, prp->pr_remid,
					SIGKILL);
				do_sleep = 1;
				prp->pr_exitcode = SIGKILL << 8;
#endif		/* -WIN32-CUT-LINE- */
				break;
			default:
				prperror(prp, *prp->pr_sys,
					errno ? errno : tet_tcerrno,
					"tcc_waitnohang() failed for pid",
					tet_l2a(prp->pr_remid));
				prp->pr_exitcode = 0177776;
				break;
			}
			break;
		default:
			/* this "can't happen" */
			fatal(0, "unexpected tool state",
				prtoolstate(prp->pr_toolstate));
			/* NOTREACHED */
		}
	}
	else
		prp->pr_exitcode = (((unsigned) status >> 8) & 0377) |
			((status & 0377) << 8);

	switch (prp->pr_jnlstatus) {
	case 0:
	case TET_ESTAT_ERROR:
		if (prp->pr_exitcode)
			prp->pr_jnlstatus = prp->pr_exitcode;
		break;
	}

	return(0);
}


#ifndef TET_LITE	/* -START-LITE-CUT- */
/*
**	quick_wtc2() - second part of quick_waittc() processing
**
**	this function is called if a test case proctab has child proctabs
**	but a single journal
**
**	gather each child proctab journal status into the journal status at
**	this level
*/

static void quick_wtc2(prp)
struct proctab *prp;
{
	struct proctab *child;

	TRACE3(TET_MAX(tet_Ttcc, tet_Texec), 6,
		"call to quick_wtc2(%s), flags = %s",
		tet_i2x(prp), prpflags(prp->pr_flags));

	ASSERT(prp->pr_child);
	ASSERT(prp->pr_jnlstatus == 0 || prp->pr_jnlstatus == TET_ESTAT_ERROR);
	ASSERT((prp->pr_flags & PRF_JNL_CHILD) == 0);

	/* first, try to find a +ve journal status (from a test case) */
	for (child = prp->pr_child; child; child = child->pr_lforw) {
		switch (child->pr_toolstate) {
		case PTS_UNWAITEDFOR:
		case PTS_KILLED_AND_UNWAITEDFOR:
			break;
		default:
			continue;
		}
		switch (prp->pr_jnlstatus) {
		case 0:
		case TET_ESTAT_ERROR:
			if (child->pr_jnlstatus > 0) {
				prp->pr_jnlstatus = child->pr_jnlstatus;
				break;
			}
			continue;
		}
		break;
	}

	/* return now if we've found it */
	switch (prp->pr_jnlstatus) {
	case 0:
	case TET_ESTAT_ERROR:
		break;
	default:
		return;
	}

	/* if not found, look for a tcc-generated error indication */
	for (child = prp->pr_child; child; child = child->pr_lforw) {
		switch (child->pr_toolstate) {
		case PTS_UNWAITEDFOR:
		case PTS_KILLED_AND_UNWAITEDFOR:
			break;
		default:
			continue;
		}
		switch (prp->pr_jnlstatus) {
		case 0:
		case TET_ESTAT_ERROR:
			if (child->pr_jnlstatus) {
				prp->pr_jnlstatus = child->pr_jnlstatus;
				break;
			}
			continue;
		}
		break;
	}
}

#endif			/* -END-LITE-CUT- */

/*
**	quick_wtc3() - third part of quick_waittc() processing
**
**	reset the toolstate to PTS_EXITED after the contrived journal status
**	has been determined
**
**	may be called for any toolstate, so must only change the
**	toolstate if it is one of those set in quick_waittc()
**
**	always returns 0
*/

static int quick_wtc3(prp)
struct proctab *prp;
{
	switch (prp->pr_toolstate) {
	case PTS_UNWAITEDFOR:
	case PTS_KILLED_AND_UNWAITEDFOR:
		prp->pr_toolstate = PTS_EXITED;
		break;
	}

	return(0);
}

/*
**	install_handler() - install a signal handler
*/

static void (*install_handler(sig, func))()
int sig;
void (*func) PROTOLIST((int));
{
	void (*rc) PROTOLIST((int));

#ifdef _WIN32	/* -START-WIN32-CUT- */

	if ((rc = signal(sig, SIG_IGN)) != SIG_IGN &&
		signal(sig, func) == SIG_ERR)
			fatal(errno, "can't install handler for signal",
				tet_i2a(sig));

#else		/* -END-WIN32-CUT- */

	struct sigaction sa;

	if (sigaction(sig, (struct sigaction *) 0, &sa) < 0)
		fatal(errno, "can't get disposition for signal", tet_i2a(sig));

	if ((rc = sa.sa_handler) != SIG_IGN) {
		sa.sa_handler = func;
		sa.sa_flags = 0;
		(void) sigemptyset(&sa.sa_mask); 
		if (sigaction(sig, &sa, (struct sigaction *) 0) < 0)
			fatal(errno, "can't install handler for signal",
				tet_i2a(sig));
	}

#endif		/* -WIN32-CUT-LINE- */

	return(rc);
}


#ifndef _WIN32		/* -WIN32-CUT-LINE- */
#  ifdef TET_LITE	/* -LITE-CUT-LINE- */

/*
**	tcc_exec_signals() - restore original signal dispositions
**		in the child process before an exec
**
**	this function is called from the tcclib function tcf_exec()
*/

void tcc_exec_signals()
{
	tes2(SIGINT, orig_sigint);
	tes2(SIGHUP, orig_sighup);
	tes2(SIGQUIT, orig_sigquit);
	tes2(SIGPIPE, orig_sigpipe);
	tes2(SIGTERM, orig_sigterm);
}

/*
**	tes2() - extend the tcc_exec_signals() processing for a
**		single signal
*/
static void tes2(sig, func)
int sig;
void (*func) PROTOLIST((int));
{
	struct sigaction sa;

	/* ignore the signal */
	sa.sa_handler = SIG_IGN;
	sa.sa_flags = 0;
	(void) sigemptyset(&sa.sa_mask); 
	if (sigaction(sig, &sa, (struct sigaction *) 0) < 0)
		fatal(errno, "sigaction(SIG_IGN) failed for signal",
			tet_i2a(sig));

	/* unblock the signal */
	(void) sigemptyset(&sa.sa_mask);
	(void) sigaddset(&sa.sa_mask, sig);
	if (sigprocmask(SIG_UNBLOCK, &sa.sa_mask, (sigset_t *) 0) < 0)
		fatal(errno, "sigprocmask(SIG_UNBLOCK) failed for signal",
			tet_i2a(sig));

	/* restore the original signal disposition if not SIG_IGN */
	if (func != SIG_IGN) {
		sa.sa_handler = func;
		sa.sa_flags = 0;
		(void) sigemptyset(&sa.sa_mask); 
		if (sigaction(sig, &sa, (struct sigaction *) 0) < 0)
			fatal(errno, "sigaction() failed for signal",
				tet_i2a(sig));

	}
}

#  endif /* TET_LITE */	/* -LITE-CUT-LINE- */
#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

